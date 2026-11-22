use std::f32::consts::PI as F32_PI;

pub fn process_eq_pcm16(
    input: &[u8],
    output: &mut [u8],
    frame_count: usize,
    channels: usize,
    coeffs: &[f64],
    states: &mut [f64],
    preamp_gain: f64,
) -> bool {
    if channels != 1 && channels != 2 {
        return false;
    }
    let band_count = coeffs.len() / 5;
    if band_count == 0 || coeffs.len() < band_count * 5 || states.len() < band_count * 8 {
        return false;
    }
    let byte_len = match frame_count.checked_mul(channels).and_then(|n| n.checked_mul(2)) {
        Some(len) => len,
        None => return false,
    };
    if input.len() < byte_len || output.len() < byte_len {
        return false;
    }

    if channels == 1 {
        for frame in 0..frame_count {
            let off = frame * 2;
            let raw = i16::from_le_bytes([input[off], input[off + 1]]) as f64;
            let mut sample = raw / 32768.0;

            for b in 0..band_count {
                let c = b * 5;
                let s = b * 8;
                let y = coeffs[c] * sample
                    + coeffs[c + 1] * states[s]
                    + coeffs[c + 2] * states[s + 1]
                    - coeffs[c + 3] * states[s + 2]
                    - coeffs[c + 4] * states[s + 3];
                states[s + 1] = states[s];
                states[s] = sample;
                states[s + 3] = states[s + 2];
                states[s + 2] = y;
                sample = y;
            }

            let out_sample = ((sample * preamp_gain * 32768.0).clamp(-32768.0, 32767.0) as i32) as i16;
            output[off..off + 2].copy_from_slice(&out_sample.to_le_bytes());
        }
    } else {
        for frame in 0..frame_count {
            let off = frame * 4;
            let raw_l = i16::from_le_bytes([input[off], input[off + 1]]) as f64;
            let raw_r = i16::from_le_bytes([input[off + 2], input[off + 3]]) as f64;
            let mut left = raw_l / 32768.0;
            let mut right = raw_r / 32768.0;

            for b in 0..band_count {
                let c = b * 5;
                let s = b * 8;
                let b0 = coeffs[c];
                let b1 = coeffs[c + 1];
                let b2 = coeffs[c + 2];
                let a1 = coeffs[c + 3];
                let a2 = coeffs[c + 4];

                let out_l = b0 * left
                    + b1 * states[s]
                    + b2 * states[s + 1]
                    - a1 * states[s + 2]
                    - a2 * states[s + 3];
                states[s + 1] = states[s];
                states[s] = left;
                states[s + 3] = states[s + 2];
                states[s + 2] = out_l;
                left = out_l;

                let out_r = b0 * right
                    + b1 * states[s + 4]
                    + b2 * states[s + 5]
                    - a1 * states[s + 6]
                    - a2 * states[s + 7];
                states[s + 5] = states[s + 4];
                states[s + 4] = right;
                states[s + 7] = states[s + 6];
                states[s + 6] = out_r;
                right = out_r;
            }

            let out_l_pcm = ((left * preamp_gain * 32768.0).clamp(-32768.0, 32767.0) as i32) as i16;
            let out_r_pcm = ((right * preamp_gain * 32768.0).clamp(-32768.0, 32767.0) as i32) as i16;
            output[off..off + 2].copy_from_slice(&out_l_pcm.to_le_bytes());
            output[off + 2..off + 4].copy_from_slice(&out_r_pcm.to_le_bytes());
        }
    }

    true
}

pub fn process_gain(
    input: &[u8],
    output: &mut [u8],
    sample_count: usize,
    encoding: i32,
    linear_gain: f64,
) -> bool {
    let bps = match encoding {
        1 => 2usize,
        2 => 3usize,
        3 | 4 => 4usize,
        _ => return false,
    };
    let byte_len = match sample_count.checked_mul(bps) {
        Some(len) => len,
        None => return false,
    };
    if input.len() < byte_len || output.len() < byte_len {
        return false;
    }

    match encoding {
        1 => {
            for i in 0..sample_count {
                let off = i * 2;
                let sample = i16::from_le_bytes([input[off], input[off + 1]]) as f64;
                let out = ((sample * linear_gain).clamp(-32768.0, 32767.0) as i32) as i16;
                output[off..off + 2].copy_from_slice(&out.to_le_bytes());
            }
        }
        2 => {
            for i in 0..sample_count {
                let off = i * 3;
                let b0 = input[off] as i32;
                let b1 = input[off + 1] as i32;
                let b2 = (input[off + 2] as i8) as i32;
                let sample = ((b2 << 16) | (b1 << 8) | b0) as f64;
                let processed = (sample * linear_gain).clamp(-8388608.0, 8388607.0) as i32;
                output[off] = (processed & 0xFF) as u8;
                output[off + 1] = ((processed >> 8) & 0xFF) as u8;
                output[off + 2] = ((processed >> 16) & 0xFF) as u8;
            }
        }
        3 => {
            for i in 0..sample_count {
                let off = i * 4;
                let sample = i32::from_le_bytes([input[off], input[off + 1], input[off + 2], input[off + 3]]) as f64;
                let out = ((sample * linear_gain).clamp(-2147483648.0, 2147483647.0) as i64) as i32;
                output[off..off + 4].copy_from_slice(&out.to_le_bytes());
            }
        }
        4 => {
            let gain_f = linear_gain as f32;
            for i in 0..sample_count {
                let off = i * 4;
                let sample = f32::from_le_bytes([input[off], input[off + 1], input[off + 2], input[off + 3]]);
                let out = (sample * gain_f).clamp(-1.0f32, 1.0f32);
                output[off..off + 4].copy_from_slice(&out.to_le_bytes());
            }
        }
        _ => return false,
    }

    true
}

struct CombFilter {
    buffer: Vec<f32>,
    buf_idx: usize,
    filter_store: f32,
}

impl CombFilter {
    fn new(size: usize) -> Self {
        Self {
            buffer: vec![0.0; size],
            buf_idx: 0,
            filter_store: 0.0,
        }
    }

    fn reset(&mut self) {
        self.buffer.fill(0.0);
        self.buf_idx = 0;
        self.filter_store = 0.0;
    }

    #[inline]
    fn process(&mut self, input: f32, fb: f32, dm: f32) -> f32 {
        let output = self.buffer[self.buf_idx];
        self.filter_store = (output * (1.0 - dm)) + (self.filter_store * dm);
        if self.filter_store > -1e-15 && self.filter_store < 1e-15 {
            self.filter_store = 0.0;
        }
        let new_sample = input + (self.filter_store * fb);
        self.buffer[self.buf_idx] = if new_sample.is_finite() { new_sample } else { 0.0 };
        self.buf_idx += 1;
        if self.buf_idx >= self.buffer.len() {
            self.buf_idx = 0;
        }
        output
    }
}

struct AllPassFilter {
    buffer: Vec<f32>,
    buf_idx: usize,
}

impl AllPassFilter {
    fn new(size: usize) -> Self {
        Self {
            buffer: vec![0.0; size],
            buf_idx: 0,
        }
    }

    fn reset(&mut self) {
        self.buffer.fill(0.0);
        self.buf_idx = 0;
    }

    #[inline]
    fn process(&mut self, input: f32) -> f32 {
        let buf_out = self.buffer[self.buf_idx];
        let output = -input + buf_out;
        let new_sample = input + (buf_out * 0.5);
        self.buffer[self.buf_idx] = if new_sample.is_finite() { new_sample } else { 0.0 };
        self.buf_idx += 1;
        if self.buf_idx >= self.buffer.len() {
            self.buf_idx = 0;
        }
        output
    }
}

pub struct NativeReverb {
    combs_l: Vec<CombFilter>,
    combs_r: Vec<CombFilter>,
    allpass_l: Vec<AllPassFilter>,
    allpass_r: Vec<AllPassFilter>,
}

impl NativeReverb {
    pub fn new(sample_rate: i32) -> Self {
        let rate = sample_rate.max(8000) as f64;
        let scale = rate / 44100.0;
        const COMB_TUNINGS: [i32; 8] = [1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617];
        const ALLPASS_TUNINGS: [i32; 4] = [556, 441, 341, 225];
        const STEREO_SPREAD: i32 = 23;

        let combs_l = COMB_TUNINGS
            .iter()
            .map(|&t| CombFilter::new(((t as f64 * scale).round() as usize).max(16)))
            .collect();
        let combs_r = COMB_TUNINGS
            .iter()
            .map(|&t| CombFilter::new((((t + STEREO_SPREAD) as f64 * scale).round() as usize).max(16)))
            .collect();
        let allpass_l = ALLPASS_TUNINGS
            .iter()
            .map(|&t| AllPassFilter::new(((t as f64 * scale).round() as usize).max(8)))
            .collect();
        let allpass_r = ALLPASS_TUNINGS
            .iter()
            .map(|&t| AllPassFilter::new((((t + STEREO_SPREAD) as f64 * scale).round() as usize).max(8)))
            .collect();

        Self {
            combs_l,
            combs_r,
            allpass_l,
            allpass_r,
        }
    }

    pub fn reset(&mut self) {
        for c in &mut self.combs_l {
            c.reset();
        }
        for c in &mut self.combs_r {
            c.reset();
        }
        for a in &mut self.allpass_l {
            a.reset();
        }
        for a in &mut self.allpass_r {
            a.reset();
        }
    }

    pub fn process_pcm16(
        &mut self,
        input: &[u8],
        output: &mut [u8],
        frame_count: usize,
        channels: usize,
        fb: f32,
        damp: f32,
        dry: f32,
        wet: f32,
        wet1: f32,
        wet2: f32,
    ) -> bool {
        if channels != 1 && channels != 2 {
            return false;
        }
        let byte_len = match frame_count.checked_mul(channels).and_then(|n| n.checked_mul(2)) {
            Some(len) => len,
            None => return false,
        };
        if input.len() < byte_len || output.len() < byte_len {
            return false;
        }

        if channels == 1 {
            for frame in 0..frame_count {
                let off = frame * 2;
                let mut in_mono = (i16::from_le_bytes([input[off], input[off + 1]]) as f32) / 32768.0;
                if !in_mono.is_finite() {
                    in_mono = 0.0;
                }

                let mut out_l = 0.0f32;
                let mut out_r = 0.0f32;
                for i in 0..self.combs_l.len() {
                    out_l += self.combs_l[i].process(in_mono, fb, damp);
                    out_r += self.combs_r[i].process(in_mono, fb, damp);
                }
                for j in 0..self.allpass_l.len() {
                    out_l = self.allpass_l[j].process(out_l);
                    out_r = self.allpass_r[j].process(out_r);
                }

                let mut final_mono = dry * in_mono + wet * (out_l + out_r) * 0.5;
                if !final_mono.is_finite() {
                    final_mono = 0.0;
                }
                let pcm = ((final_mono * 32768.0).clamp(-32768.0, 32767.0) as i32) as i16;
                output[off..off + 2].copy_from_slice(&pcm.to_le_bytes());
            }
        } else {
            for frame in 0..frame_count {
                let off = frame * 4;
                let mut in_l = (i16::from_le_bytes([input[off], input[off + 1]]) as f32) / 32768.0;
                let mut in_r = (i16::from_le_bytes([input[off + 2], input[off + 3]]) as f32) / 32768.0;
                if !in_l.is_finite() {
                    in_l = 0.0;
                }
                if !in_r.is_finite() {
                    in_r = 0.0;
                }

                let mixed = (in_l + in_r) * 0.5;
                let mut out_l = 0.0f32;
                let mut out_r = 0.0f32;
                for i in 0..self.combs_l.len() {
                    out_l += self.combs_l[i].process(mixed, fb, damp);
                    out_r += self.combs_r[i].process(mixed, fb, damp);
                }
                for j in 0..self.allpass_l.len() {
                    out_l = self.allpass_l[j].process(out_l);
                    out_r = self.allpass_r[j].process(out_r);
                }

                let mut final_l = dry * in_l + wet1 * out_l + wet2 * out_r;
                let mut final_r = dry * in_r + wet1 * out_r + wet2 * out_l;
                if !final_l.is_finite() {
                    final_l = 0.0;
                }
                if !final_r.is_finite() {
                    final_r = 0.0;
                }

                let pcm_l = ((final_l * 32768.0).clamp(-32768.0, 32767.0) as i32) as i16;
                let pcm_r = ((final_r * 32768.0).clamp(-32768.0, 32767.0) as i32) as i16;
                output[off..off + 2].copy_from_slice(&pcm_l.to_le_bytes());
                output[off + 2..off + 4].copy_from_slice(&pcm_r.to_le_bytes());
            }
        }

        true
    }
}

pub fn compute_spectrum_bars(
    input: &[u8],
    channels: usize,
    sample_rate: i32,
    out_bars: &mut [f32],
) -> bool {
    const N: usize = 256;
    if (channels != 1 && channels != 2) || sample_rate <= 0 || out_bars.is_empty() {
        return false;
    }

    let frame_bytes = channels * 2;
    let available_frames = input.len() / frame_bytes;
    if available_frames == 0 {
        out_bars.fill(0.0);
        return true;
    }
    let count = available_frames.min(N);

    let mut re = [0.0f32; N];
    let mut im = [0.0f32; N];

    for i in 0..count {
        let off = i * frame_bytes;
        let mono = if channels == 1 {
            (i16::from_le_bytes([input[off], input[off + 1]]) as f32) / 32768.0
        } else {
            let l = i16::from_le_bytes([input[off], input[off + 1]]) as f32;
            let r = i16::from_le_bytes([input[off + 2], input[off + 3]]) as f32;
            (l + r) * (0.5 / 32768.0)
        };
        let win = 0.5 * (1.0 - (2.0 * F32_PI * (i as f32) / ((N - 1) as f32)).cos());
        re[i] = mono * win;
    }

    let mut j = 0usize;
    for i in 1..N {
        let mut bit = N >> 1;
        while (j & bit) != 0 {
            j ^= bit;
            bit >>= 1;
        }
        j ^= bit;
        if i < j {
            re.swap(i, j);
            im.swap(i, j);
        }
    }

    let mut len = 2usize;
    while len <= N {
        let half = len >> 1;
        let ang = -F32_PI / (half as f32);
        let w_base_re = ang.cos();
        let w_base_im = ang.sin();
        let mut i = 0usize;
        while i < N {
            let mut w_re = 1.0f32;
            let mut w_im = 0.0f32;
            for k in 0..half {
                let u = i + k;
                let v = u + half;
                let even_re = re[u];
                let even_im = im[u];
                let odd_re = re[v] * w_re - im[v] * w_im;
                let odd_im = re[v] * w_im + im[v] * w_re;
                re[u] = even_re + odd_re;
                im[u] = even_im + odd_im;
                re[v] = even_re - odd_re;
                im[v] = even_im - odd_im;
                let next_w_re = w_re * w_base_re - w_im * w_base_im;
                w_im = w_re * w_base_im + w_im * w_base_re;
                w_re = next_w_re;
            }
            i += len;
        }
        len <<= 1;
    }

    let mut mag = [0.0f32; 128];
    for bin in 1..128 {
        mag[bin] = (re[bin] * re[bin] + im[bin] * im[bin]).sqrt() / 64.0;
    }

    let sr = sample_rate as f32;
    let hi_limit = (sr * 0.48).min(14000.0).max(120.0);
    let lo_limit = 60.0f32.min(hi_limit * 0.5);
    let ratio = hi_limit / lo_limit;
    let bar_count = out_bars.len() as f32;
    let norm_denom = 13.0f32.ln();

    for (idx, bar) in out_bars.iter_mut().enumerate() {
        let f_lo = lo_limit * ratio.powf((idx as f32) / bar_count);
        let f_hi = lo_limit * ratio.powf(((idx + 1) as f32) / bar_count);
        let b_lo = ((f_lo * (N as f32) / sr).floor() as isize).clamp(1, 127) as usize;
        let b_hi = ((f_hi * (N as f32) / sr).ceil() as isize).clamp(b_lo as isize, 127) as usize;
        let mut peak = 0.0f32;
        for bin in b_lo..=b_hi {
            if mag[bin] > peak {
                peak = mag[bin];
            }
        }
        let compressed = (1.0 + peak * 12.0).ln() / norm_denom;
        *bar = compressed.clamp(0.0, 1.0);
    }

    true
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn eq_unity_filter_matches_preamp_gain() {
        let coeffs = [1.0, 0.0, 0.0, 0.0, 0.0];
        let mut states = [0.0; 8];
        let mut input = Vec::new();
        for s in [1000i16, -2000i16, 4000i16, -4000i16] {
            input.extend_from_slice(&s.to_le_bytes());
        }
        let mut output = vec![0u8; input.len()];
        assert!(process_eq_pcm16(&input, &mut output, 2, 2, &coeffs, &mut states, 0.5));
        assert_eq!(i16::from_le_bytes([output[0], output[1]]), 500);
        assert_eq!(i16::from_le_bytes([output[2], output[3]]), -1000);
        assert_eq!(i16::from_le_bytes([output[4], output[5]]), 2000);
        assert_eq!(i16::from_le_bytes([output[6], output[7]]), -2000);
    }

    #[test]
    fn gain_scales_and_clamps_all_encodings() {
        let pcm16: Vec<u8> = [10000i16, -30000i16]
            .iter()
            .flat_map(|s| s.to_le_bytes())
            .collect();
        let mut out16 = vec![0u8; 4];
        assert!(process_gain(&pcm16, &mut out16, 2, 1, 2.0));
        assert_eq!(i16::from_le_bytes([out16[0], out16[1]]), 20000);
        assert_eq!(i16::from_le_bytes([out16[2], out16[3]]), -32768);

        let val24_neg = -5_000_000i32;
        let pcm24 = vec![
            (val24_neg & 0xFF) as u8,
            ((val24_neg >> 8) & 0xFF) as u8,
            ((val24_neg >> 16) & 0xFF) as u8,
        ];
        let mut out24 = vec![0u8; 3];
        assert!(process_gain(&pcm24, &mut out24, 1, 2, 2.0));
        let clamped = ((out24[2] as i8 as i32) << 16) | ((out24[1] as i32) << 8) | (out24[0] as i32);
        assert_eq!(clamped, -8_388_608);

        let pcm32: Vec<u8> = [1_000_000_000i32, -1_500_000_000i32]
            .iter()
            .flat_map(|s| s.to_le_bytes())
            .collect();
        let mut out32 = vec![0u8; 8];
        assert!(process_gain(&pcm32, &mut out32, 2, 3, 2.0));
        assert_eq!(i32::from_le_bytes([out32[0], out32[1], out32[2], out32[3]]), 2_000_000_000);
        assert_eq!(i32::from_le_bytes([out32[4], out32[5], out32[6], out32[7]]), i32::MIN);

        let pcm_f: Vec<u8> = [0.25f32, -0.8f32]
            .iter()
            .flat_map(|s| s.to_le_bytes())
            .collect();
        let mut out_f = vec![0u8; 8];
        assert!(process_gain(&pcm_f, &mut out_f, 2, 4, 2.0));
        assert!((f32::from_le_bytes([out_f[0], out_f[1], out_f[2], out_f[3]]) - 0.5).abs() < 1e-6);
        assert_eq!(f32::from_le_bytes([out_f[4], out_f[5], out_f[6], out_f[7]]), -1.0);
    }

    #[test]
    fn reverb_produces_finite_stereo_output() {
        let mut rev = NativeReverb::new(48_000);
        let mut input = vec![0u8; 4096 * 4];
        input[0..2].copy_from_slice(&16000i16.to_le_bytes());
        input[2..4].copy_from_slice(&(-16000i16).to_le_bytes());
        let mut output = vec![0u8; input.len()];
        assert!(rev.process_pcm16(&input, &mut output, 4096, 2, 0.84, 0.2, 1.0, 0.35, 0.35, 0.0));
        assert_eq!(i16::from_le_bytes([output[0], output[1]]), 16000);
        rev.reset();
    }

    #[test]
    fn spectrum_bars_detect_low_vs_high_frequency() {
        let sr = 48_000.0f32;
        let mut low_pcm = Vec::with_capacity(256 * 2);
        let mut high_pcm = Vec::with_capacity(256 * 2);
        for i in 0..256 {
            let t = i as f32 / sr;
            let low = ((2.0 * F32_PI * 220.0 * t).sin() * 24000.0) as i16;
            let high = ((2.0 * F32_PI * 6000.0 * t).sin() * 24000.0) as i16;
            low_pcm.extend_from_slice(&low.to_le_bytes());
            high_pcm.extend_from_slice(&high.to_le_bytes());
        }
        let mut low_bars = [0.0f32; 24];
        let mut high_bars = [0.0f32; 24];
        assert!(compute_spectrum_bars(&low_pcm, 1, 48_000, &mut low_bars));
        assert!(compute_spectrum_bars(&high_pcm, 1, 48_000, &mut high_bars));

        let low_peak_idx = low_bars
            .iter()
            .enumerate()
            .max_by(|a, b| a.1.partial_cmp(b.1).unwrap())
            .unwrap()
            .0;
        let high_peak_idx = high_bars
            .iter()
            .enumerate()
            .max_by(|a, b| a.1.partial_cmp(b.1).unwrap())
            .unwrap()
            .0;
        assert!(low_peak_idx < 10);
        assert!(high_peak_idx > 14);
    }
}
