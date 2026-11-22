use std::f64::consts::PI;

const SAMPLE_RATE: usize = 16_000;
const FFT_SIZE: usize = 2048;
const FFT_OUTPUT_SIZE: usize = FFT_SIZE / 2 + 1;
const MAX_PEAKS: usize = 255;
const MAX_TIME_SECONDS: f64 = 12.0;
const RING_BUF_SIZE: usize = 256;

struct FrequencyPeak {
    fft_pass_number: i32,
    peak_magnitude: i32,
    corrected_bin: i32,
}

struct SignatureGeneratorState {
    hanning: [f64; FFT_SIZE],
    samples_ring: [i32; FFT_SIZE],
    samples_pos: usize,
    fft_outputs: Vec<[f64; FFT_OUTPUT_SIZE]>,
    fft_pos: usize,
    fft_num_written: i32,
    spread_ffts: Vec<[f64; FFT_OUTPUT_SIZE]>,
    spread_pos: usize,
    spread_num_written: i32,
    num_samples: usize,
    band_peaks: [Vec<FrequencyPeak>; 4],
    total_peaks: usize,
    fft_re: [f64; FFT_SIZE],
    fft_im: [f64; FFT_SIZE],
}

impl SignatureGeneratorState {
    fn new() -> Self {
        let mut hanning = [0.0f64; FFT_SIZE];
        for i in 0..FFT_SIZE {
            hanning[i] = 0.5 * (1.0 - (2.0 * PI * ((i + 1) as f64) / 2049.0).cos());
        }
        Self {
            hanning,
            samples_ring: [0i32; FFT_SIZE],
            samples_pos: 0,
            fft_outputs: vec![[0.0f64; FFT_OUTPUT_SIZE]; RING_BUF_SIZE],
            fft_pos: 0,
            fft_num_written: 0,
            spread_ffts: vec![[0.0f64; FFT_OUTPUT_SIZE]; RING_BUF_SIZE],
            spread_pos: 0,
            spread_num_written: 0,
            num_samples: 0,
            band_peaks: [Vec::new(), Vec::new(), Vec::new(), Vec::new()],
            total_peaks: 0,
            fft_re: [0.0f64; FFT_SIZE],
            fft_im: [0.0f64; FFT_SIZE],
        }
    }

    fn process(&mut self, samples: &[u8]) -> String {
        let total_pcm = samples.len() / 2;
        let mut offset = 0usize;
        while offset + 128 <= total_pcm {
            let elapsed_sec = (self.num_samples as f64) / (SAMPLE_RATE as f64);
            if elapsed_sec >= MAX_TIME_SECONDS && self.total_peaks >= MAX_PEAKS {
                break;
            }
            self.num_samples += 128;
            self.feed_samples(samples, offset, 128);
            self.do_fft();
            self.do_peak_spreading_and_recognition();
            offset += 128;
        }
        self.encode_signature()
    }

    fn feed_samples(&mut self, samples: &[u8], start: usize, count: usize) {
        for k in start..(start + count) {
            let base = k * 2;
            let sample = i16::from_le_bytes([samples[base], samples[base + 1]]) as i32;
            self.samples_ring[self.samples_pos] = sample;
            self.samples_pos = (self.samples_pos + 1) % FFT_SIZE;
        }
    }

    fn do_fft(&mut self) {
        for i in 0..FFT_SIZE {
            let s = self.samples_ring[(self.samples_pos + i) % FFT_SIZE] as f64;
            self.fft_re[i] = s * self.hanning[i];
            self.fft_im[i] = 0.0;
        }

        let mut j = 0usize;
        for i in 1..FFT_SIZE {
            let mut bit = FFT_SIZE >> 1;
            while (j & bit) != 0 {
                j ^= bit;
                bit >>= 1;
            }
            j ^= bit;
            if i < j {
                self.fft_re.swap(i, j);
                self.fft_im.swap(i, j);
            }
        }

        let mut len = 2usize;
        while len <= FFT_SIZE {
            let half_len = len >> 1;
            let ang = -PI / (half_len as f64);
            let w_base_re = ang.cos();
            let w_base_im = ang.sin();
            let mut i = 0usize;
            while i < FFT_SIZE {
                let mut w_re = 1.0f64;
                let mut w_im = 0.0f64;
                for k in 0..half_len {
                    let u = i + k;
                    let v = u + half_len;
                    let even_re = self.fft_re[u];
                    let even_im = self.fft_im[u];
                    let odd_re = self.fft_re[v] * w_re - self.fft_im[v] * w_im;
                    let odd_im = self.fft_re[v] * w_im + self.fft_im[v] * w_re;
                    self.fft_re[u] = even_re + odd_re;
                    self.fft_im[u] = even_im + odd_im;
                    self.fft_re[v] = even_re - odd_re;
                    self.fft_im[v] = even_im - odd_im;
                    let new_w_re = w_re * w_base_re - w_im * w_base_im;
                    w_im = w_re * w_base_im + w_im * w_base_re;
                    w_re = new_w_re;
                }
                i += len;
            }
            len <<= 1;
        }

        let scale_factor = 1.0f64 / ((1u32 << 17) as f64);
        let min_val = 1e-10f64;
        let out = &mut self.fft_outputs[self.fft_pos];
        for idx in 0..FFT_OUTPUT_SIZE {
            let r = self.fft_re[idx];
            let img = self.fft_im[idx];
            let mag = (r * r + img * img) * scale_factor;
            out[idx] = if mag < min_val { min_val } else { mag };
        }
        self.fft_pos = (self.fft_pos + 1) % RING_BUF_SIZE;
        self.fft_num_written += 1;
    }

    fn do_peak_spreading_and_recognition(&mut self) {
        self.do_peak_spreading();
        if self.spread_num_written >= 47 {
            self.do_peak_recognition();
        }
    }

    fn do_peak_spreading(&mut self) {
        let last_fft_idx = (self.fft_pos + RING_BUF_SIZE - 1) % RING_BUF_SIZE;
        let mut spread = self.fft_outputs[last_fft_idx];

        for pos in 0..(FFT_OUTPUT_SIZE - 2) {
            let mut m = spread[pos];
            if spread[pos + 1] > m {
                m = spread[pos + 1];
            }
            if spread[pos + 2] > m {
                m = spread[pos + 2];
            }
            spread[pos] = m;
        }

        for pos in 0..FFT_OUTPUT_SIZE {
            let mut max_val = spread[pos];
            for &offset in &[-1isize, -3, -6] {
                let idx =
                    ((self.spread_pos as isize + offset).rem_euclid(RING_BUF_SIZE as isize))
                        as usize;
                let old_val = self.spread_ffts[idx][pos];
                if old_val > max_val {
                    max_val = old_val;
                }
                self.spread_ffts[idx][pos] = max_val;
            }
        }

        self.spread_ffts[self.spread_pos] = spread;
        self.spread_pos = (self.spread_pos + 1) % RING_BUF_SIZE;
        self.spread_num_written += 1;
    }

    fn do_peak_recognition(&mut self) {
        let fft_minus_46 =
            &self.fft_outputs[(self.fft_pos + RING_BUF_SIZE * 2 - 46) % RING_BUF_SIZE];
        let spread_minus_49 =
            &self.spread_ffts[(self.spread_pos + RING_BUF_SIZE * 2 - 49) % RING_BUF_SIZE];

        const OTHER_OFFSETS: [isize; 14] = [
            -53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249,
        ];
        const NEIGHBOR_OFFSETS: [isize; 8] = [-10, -7, -4, -3, 1, 2, 5, 8];

        for bin_pos in 10..(FFT_OUTPUT_SIZE - 8) {
            let fft_val = fft_minus_46[bin_pos];
            if fft_val < (1.0 / 64.0) || fft_val < spread_minus_49[bin_pos] {
                continue;
            }

            let mut max_neighbor_spread49 = 0.0f64;
            for &neighbor_offset in &NEIGHBOR_OFFSETS {
                let idx = (bin_pos as isize + neighbor_offset) as usize;
                let v = spread_minus_49[idx];
                if v > max_neighbor_spread49 {
                    max_neighbor_spread49 = v;
                }
            }
            if fft_val <= max_neighbor_spread49 {
                continue;
            }

            let mut max_neighbor_other = max_neighbor_spread49;
            for &other_offset in &OTHER_OFFSETS {
                let spread_idx = ((self.spread_pos as isize + other_offset)
                    .rem_euclid(RING_BUF_SIZE as isize)) as usize;
                let v = self.spread_ffts[spread_idx][bin_pos - 1];
                if v > max_neighbor_other {
                    max_neighbor_other = v;
                }
            }
            if fft_val <= max_neighbor_other {
                continue;
            }

            let fft_number = self.spread_num_written - 46;
            let floor = 1.0f64 / 64.0f64;
            let peak_mag = fft_val.max(floor).ln() * 1477.3 + 6144.0;
            let peak_mag_before = fft_minus_46[bin_pos - 1].max(floor).ln() * 1477.3 + 6144.0;
            let peak_mag_after = fft_minus_46[bin_pos + 1].max(floor).ln() * 1477.3 + 6144.0;

            let peak_variation1 = peak_mag * 2.0 - peak_mag_before - peak_mag_after;
            let peak_variation2 = (peak_mag_after - peak_mag_before) * 32.0 / peak_variation1;

            let corrected_bin = (bin_pos as f64) * 64.0 + peak_variation2;
            let frequency_hz = corrected_bin * (16000.0 / 2.0 / 1024.0 / 64.0);

            let band = if frequency_hz < 250.0 {
                continue;
            } else if frequency_hz < 520.0 {
                0
            } else if frequency_hz < 1450.0 {
                1
            } else if frequency_hz < 3500.0 {
                2
            } else if frequency_hz <= 5500.0 {
                3
            } else {
                continue;
            };

            self.band_peaks[band].push(FrequencyPeak {
                fft_pass_number: fft_number,
                peak_magnitude: peak_mag as i32,
                corrected_bin: corrected_bin as i32,
            });
            self.total_peaks += 1;
        }
    }

    fn encode_signature(&self) -> String {
        let mut contents = Vec::with_capacity(2048);

        for band_id in 0..4usize {
            let peaks = &self.band_peaks[band_id];
            if peaks.is_empty() {
                continue;
            }

            let mut peak_buf = Vec::with_capacity(peaks.len() * 6);
            let mut prev_fft_pass_number = 0i32;

            for peak in peaks {
                let diff = peak.fft_pass_number - prev_fft_pass_number;
                if diff >= 255 {
                    peak_buf.push(0xFF);
                    peak_buf.extend_from_slice(&peak.fft_pass_number.to_le_bytes());
                    prev_fft_pass_number = peak.fft_pass_number;
                }
                peak_buf.push((peak.fft_pass_number - prev_fft_pass_number) as u8);
                peak_buf.extend_from_slice(&(peak.peak_magnitude as u16).to_le_bytes());
                peak_buf.extend_from_slice(&(peak.corrected_bin as u16).to_le_bytes());
                prev_fft_pass_number = peak.fft_pass_number;
            }

            contents.extend_from_slice(&(0x60030040u32 + band_id as u32).to_le_bytes());
            contents.extend_from_slice(&(peak_buf.len() as u32).to_le_bytes());
            contents.extend_from_slice(&peak_buf);

            let pad_bytes = (4 - peak_buf.len() % 4) % 4;
            contents.resize(contents.len() + pad_bytes, 0);
        }

        let size_minus_header = (contents.len() + 8) as u32;
        let samples_and_offset =
            ((self.num_samples as f64) + (SAMPLE_RATE as f64) * 0.24) as i32 as u32;

        let mut full_bytes = Vec::with_capacity(56 + contents.len());
        full_bytes.extend_from_slice(&0xcafe2580u32.to_le_bytes());
        full_bytes.extend_from_slice(&0u32.to_le_bytes());
        full_bytes.extend_from_slice(&size_minus_header.to_le_bytes());
        full_bytes.extend_from_slice(&0x94119c00u32.to_le_bytes());
        full_bytes.extend_from_slice(&0u32.to_le_bytes());
        full_bytes.extend_from_slice(&0u32.to_le_bytes());
        full_bytes.extend_from_slice(&0u32.to_le_bytes());
        full_bytes.extend_from_slice(&(3u32 << 27).to_le_bytes());
        full_bytes.extend_from_slice(&0u32.to_le_bytes());
        full_bytes.extend_from_slice(&0u32.to_le_bytes());
        full_bytes.extend_from_slice(&samples_and_offset.to_le_bytes());
        full_bytes.extend_from_slice(&((15u32 << 19) + 0x40000).to_le_bytes());

        full_bytes.extend_from_slice(&0x40000000u32.to_le_bytes());
        full_bytes.extend_from_slice(&((contents.len() + 8) as u32).to_le_bytes());
        full_bytes.extend_from_slice(&contents);

        let crc = crc32_ieee(&full_bytes[8..]);
        full_bytes[4..8].copy_from_slice(&crc.to_le_bytes());

        let mut uri = String::from("data:audio/vnd.shazam.sig;base64,");
        base64_encode_into(&full_bytes, &mut uri);
        uri
    }
}

pub fn from_i16(samples: &[u8]) -> Option<String> {
    if samples.len() < 2 || samples.len() % 2 != 0 {
        return None;
    }
    let mut state = SignatureGeneratorState::new();
    Some(state.process(samples))
}

fn crc32_ieee(data: &[u8]) -> u32 {
    let mut crc = 0xFFFF_FFFFu32;
    for &b in data {
        crc ^= b as u32;
        for _ in 0..8 {
            let mask = (crc & 1).wrapping_neg();
            crc = (crc >> 1) ^ (0xEDB8_8320u32 & mask);
        }
    }
    !crc
}

fn base64_encode_into(input: &[u8], out: &mut String) {
    const TABLE: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut i = 0usize;
    while i + 3 <= input.len() {
        let n = ((input[i] as u32) << 16) | ((input[i + 1] as u32) << 8) | (input[i + 2] as u32);
        out.push(TABLE[((n >> 18) & 0x3F) as usize] as char);
        out.push(TABLE[((n >> 12) & 0x3F) as usize] as char);
        out.push(TABLE[((n >> 6) & 0x3F) as usize] as char);
        out.push(TABLE[(n & 0x3F) as usize] as char);
        i += 3;
    }
    let rem = input.len() - i;
    if rem == 1 {
        let n = (input[i] as u32) << 16;
        out.push(TABLE[((n >> 18) & 0x3F) as usize] as char);
        out.push(TABLE[((n >> 12) & 0x3F) as usize] as char);
        out.push('=');
        out.push('=');
    } else if rem == 2 {
        let n = ((input[i] as u32) << 16) | ((input[i + 1] as u32) << 8);
        out.push(TABLE[((n >> 18) & 0x3F) as usize] as char);
        out.push(TABLE[((n >> 12) & 0x3F) as usize] as char);
        out.push(TABLE[((n >> 6) & 0x3F) as usize] as char);
        out.push('=');
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn crc32_matches_standard_vector() {
        assert_eq!(crc32_ieee(b"123456789"), 0xCBF43926);
    }

    #[test]
    fn base64_matches_rfc4648() {
        let mut s = String::new();
        base64_encode_into(b"foobar", &mut s);
        assert_eq!(s, "Zm9vYmFy");
        s.clear();
        base64_encode_into(b"foob", &mut s);
        assert_eq!(s, "Zm9vYg==");
        s.clear();
        base64_encode_into(b"fooba", &mut s);
        assert_eq!(s, "Zm9vYmE=");
    }

    #[test]
    fn rejects_invalid_pcm_lengths() {
        assert!(from_i16(&[]).is_none());
        assert!(from_i16(&[0u8]).is_none());
        assert!(from_i16(&[0u8, 1u8, 2u8]).is_none());
    }

    #[test]
    fn deterministic_multitone_signature() {
        let sample_count = 16_000 * 3;
        let mut pcm = Vec::with_capacity(sample_count * 2);
        for i in 0..sample_count {
            let t = (i as f64) / 16_000.0;
            let amp = 0.25 * (2.0 * PI * 440.0 * t).sin()
                + 0.25 * (2.0 * PI * 1000.0 * t).sin()
                + 0.25 * (2.0 * PI * 2500.0 * t).sin()
                + 0.25 * (2.0 * PI * 4200.0 * t).sin();
            let s = (amp * 20_000.0).round() as i16;
            pcm.extend_from_slice(&s.to_le_bytes());
        }
        let sig = from_i16(&pcm).expect("signature");
        assert!(sig.starts_with("data:audio/vnd.shazam.sig;base64,"));
        println!("RUST_STEADY_SIG={}", sig);

        let pulsed_count = 16_000 * 4;
        let mut pulsed_pcm = Vec::with_capacity(pulsed_count * 2);
        for i in 0..pulsed_count {
            let t = (i as f64) / 16_000.0;
            let phase = (i % 4800) as f64 / 16_000.0;
            let env = if phase < 0.08 {
                (PI * phase / 0.08).sin()
            } else {
                0.0
            };
            let amp = env
                * (0.25 * (2.0 * PI * 380.0 * t).sin()
                    + 0.25 * (2.0 * PI * 950.0 * t).sin()
                    + 0.25 * (2.0 * PI * 2400.0 * t).sin()
                    + 0.25 * (2.0 * PI * 4400.0 * t).sin());
            let s = (amp * 24_000.0).round() as i16;
            pulsed_pcm.extend_from_slice(&s.to_le_bytes());
        }
        let pulsed_sig = from_i16(&pulsed_pcm).expect("pulsed signature");
        assert!(pulsed_sig.len() > 140);
        println!("RUST_PULSED_SIG={}", pulsed_sig);
    }
}
