pub mod dsp;
pub mod lyrics;
pub mod shazam;

use jni::objects::{JByteArray, JByteBuffer, JClass, JDoubleArray, JFloatArray, JString};
use jni::sys::{jboolean, jdouble, jfloat, jint, jlong, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

fn clear_pending_exception(env: &mut JNIEnv) {
    if env.exception_check().unwrap_or(false) {
        let _ = env.exception_clear();
    }
}

fn to_jstring(env: &mut JNIEnv, s: &str) -> jstring {
    clear_pending_exception(env);
    match env.new_string(s) {
        Ok(js) => js.into_raw(),
        Err(_) => {
            clear_pending_exception(env);
            std::ptr::null_mut()
        }
    }
}

fn direct_in_slice<'a>(
    env: &JNIEnv,
    buf: &JByteBuffer,
    pos: jint,
    byte_len: usize,
) -> Option<&'a [u8]> {
    if pos < 0 {
        return None;
    }
    let ptr = env.get_direct_buffer_address(buf).ok()?;
    let cap = env.get_direct_buffer_capacity(buf).ok()?;
    if ptr.is_null() {
        return None;
    }
    let start = pos as usize;
    let end = start.checked_add(byte_len)?;
    if end > cap {
        return None;
    }
    Some(unsafe { std::slice::from_raw_parts(ptr.add(start), byte_len) })
}

fn direct_out_slice<'a>(
    env: &JNIEnv,
    buf: &JByteBuffer,
    pos: jint,
    byte_len: usize,
) -> Option<&'a mut [u8]> {
    if pos < 0 {
        return None;
    }
    let ptr = env.get_direct_buffer_address(buf).ok()?;
    let cap = env.get_direct_buffer_capacity(buf).ok()?;
    if ptr.is_null() {
        return None;
    }
    let start = pos as usize;
    let end = start.checked_add(byte_len)?;
    if end > cap {
        return None;
    }
    Some(unsafe { std::slice::from_raw_parts_mut(ptr.add(start), byte_len) })
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeGenerateShazamSignature(
    mut env: JNIEnv,
    _class: JClass,
    samples: JByteArray,
) -> jstring {
    let bytes = match env.convert_byte_array(&samples) {
        Ok(b) => b,
        Err(_) => {
            clear_pending_exception(&mut env);
            return std::ptr::null_mut();
        }
    };

    match shazam::from_i16(&bytes) {
        Some(sig) => to_jstring(&mut env, &sig),
        None => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeCleanTitleForSearch(
    mut env: JNIEnv,
    _class: JClass,
    title: JString,
) -> jstring {
    let input: String = match env.get_string(&title) {
        Ok(s) => s.into(),
        Err(_) => {
            clear_pending_exception(&mut env);
            return std::ptr::null_mut();
        }
    };

    let cleaned = lyrics::clean_title_for_search(&input);
    to_jstring(&mut env, &cleaned)
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeFilterLyricsCreditLines(
    mut env: JNIEnv,
    _class: JClass,
    lyrics_input: JString,
) -> jstring {
    let input: String = match env.get_string(&lyrics_input) {
        Ok(s) => s.into(),
        Err(_) => {
            clear_pending_exception(&mut env);
            return std::ptr::null_mut();
        }
    };

    let filtered = lyrics::filter_lyrics_credit_lines(&input);
    to_jstring(&mut env, &filtered)
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeProcessEqPcm16Direct(
    mut env: JNIEnv,
    _class: JClass,
    input: JByteBuffer,
    input_pos: jint,
    output: JByteBuffer,
    output_pos: jint,
    frame_count: jint,
    channels: jint,
    coeffs: JDoubleArray,
    states: JDoubleArray,
    preamp_gain: jdouble,
) -> jboolean {
    if frame_count <= 0 || (channels != 1 && channels != 2) {
        return JNI_FALSE;
    }
    let ch = channels as usize;
    let fc = frame_count as usize;
    let byte_len = match fc.checked_mul(ch).and_then(|n| n.checked_mul(2)) {
        Some(n) => n,
        None => return JNI_FALSE,
    };

    let coeffs_len = match env.get_array_length(&coeffs) {
        Ok(l) if l > 0 => l as usize,
        _ => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };
    let states_len = match env.get_array_length(&states) {
        Ok(l) if l > 0 => l as usize,
        _ => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };

    let in_slice = match direct_in_slice(&env, &input, input_pos, byte_len) {
        Some(s) => s,
        None => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };
    let out_slice = match direct_out_slice(&env, &output, output_pos, byte_len) {
        Some(s) => s,
        None => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };
    if std::ptr::eq(in_slice.as_ptr(), out_slice.as_ptr()) {
        return JNI_FALSE;
    }

    let ok = if coeffs_len <= 160 && states_len <= 256 {
        let mut coeffs_buf = [0.0f64; 160];
        let mut states_buf = [0.0f64; 256];
        if env.get_double_array_region(&coeffs, 0, &mut coeffs_buf[..coeffs_len]).is_err()
            || env.get_double_array_region(&states, 0, &mut states_buf[..states_len]).is_err()
        {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
        let processed = dsp::process_eq_pcm16(
            in_slice,
            out_slice,
            fc,
            ch,
            &coeffs_buf[..coeffs_len],
            &mut states_buf[..states_len],
            preamp_gain,
        );
        if processed {
            if env.set_double_array_region(&states, 0, &states_buf[..states_len]).is_err() {
                clear_pending_exception(&mut env);
                return JNI_FALSE;
            }
        }
        processed
    } else {
        let mut coeffs_vec = vec![0.0f64; coeffs_len];
        let mut states_vec = vec![0.0f64; states_len];
        if env.get_double_array_region(&coeffs, 0, &mut coeffs_vec).is_err()
            || env.get_double_array_region(&states, 0, &mut states_vec).is_err()
        {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
        let processed = dsp::process_eq_pcm16(
            in_slice,
            out_slice,
            fc,
            ch,
            &coeffs_vec,
            &mut states_vec,
            preamp_gain,
        );
        if processed {
            if env.set_double_array_region(&states, 0, &states_vec).is_err() {
                clear_pending_exception(&mut env);
                return JNI_FALSE;
            }
        }
        processed
    };

    if ok {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeProcessGainDirect(
    mut env: JNIEnv,
    _class: JClass,
    input: JByteBuffer,
    input_pos: jint,
    output: JByteBuffer,
    output_pos: jint,
    sample_count: jint,
    encoding: jint,
    linear_gain: jdouble,
) -> jboolean {
    if sample_count <= 0 {
        return JNI_FALSE;
    }
    let bps = match encoding {
        1 => 2usize,
        2 => 3usize,
        3 | 4 => 4usize,
        _ => return JNI_FALSE,
    };
    let sc = sample_count as usize;
    let byte_len = match sc.checked_mul(bps) {
        Some(n) => n,
        None => return JNI_FALSE,
    };

    let in_slice = match direct_in_slice(&env, &input, input_pos, byte_len) {
        Some(s) => s,
        None => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };
    let out_slice = match direct_out_slice(&env, &output, output_pos, byte_len) {
        Some(s) => s,
        None => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };
    if std::ptr::eq(in_slice.as_ptr(), out_slice.as_ptr()) {
        return JNI_FALSE;
    }

    if dsp::process_gain(in_slice, out_slice, sc, encoding, linear_gain) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeCreateReverb(
    _env: JNIEnv,
    _class: JClass,
    sample_rate: jint,
) -> jlong {
    if sample_rate <= 0 {
        return 0;
    }
    Box::into_raw(Box::new(dsp::NativeReverb::new(sample_rate))) as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeResetReverb(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }
    let reverb = unsafe { &mut *(handle as *mut dsp::NativeReverb) };
    reverb.reset();
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeDestroyReverb(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }
    unsafe {
        drop(Box::from_raw(handle as *mut dsp::NativeReverb));
    }
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeProcessReverbDirect(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    input: JByteBuffer,
    input_pos: jint,
    output: JByteBuffer,
    output_pos: jint,
    frame_count: jint,
    channels: jint,
    fb: jfloat,
    damp: jfloat,
    dry: jfloat,
    wet: jfloat,
    wet1: jfloat,
    wet2: jfloat,
) -> jboolean {
    if handle == 0 || frame_count <= 0 || (channels != 1 && channels != 2) {
        return JNI_FALSE;
    }
    let fc = frame_count as usize;
    let ch = channels as usize;
    let byte_len = match fc.checked_mul(ch).and_then(|n| n.checked_mul(2)) {
        Some(n) => n,
        None => return JNI_FALSE,
    };

    let in_slice = match direct_in_slice(&env, &input, input_pos, byte_len) {
        Some(s) => s,
        None => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };
    let out_slice = match direct_out_slice(&env, &output, output_pos, byte_len) {
        Some(s) => s,
        None => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };
    if std::ptr::eq(in_slice.as_ptr(), out_slice.as_ptr()) {
        return JNI_FALSE;
    }

    let reverb = unsafe { &mut *(handle as *mut dsp::NativeReverb) };
    if reverb.process_pcm16(in_slice, out_slice, fc, ch, fb, damp, dry, wet, wet1, wet2) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_sonara_music_utils_SonaraNativeCore_nativeComputeSpectrumBarsDirect(
    mut env: JNIEnv,
    _class: JClass,
    input: JByteBuffer,
    input_pos: jint,
    byte_len: jint,
    channels: jint,
    sample_rate: jint,
    out_bars: JFloatArray,
) -> jboolean {
    if byte_len <= 0 || (channels != 1 && channels != 2) || sample_rate <= 0 {
        return JNI_FALSE;
    }
    let bar_len = match env.get_array_length(&out_bars) {
        Ok(l) if l > 0 && l <= 64 => l as usize,
        _ => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };
    let in_slice = match direct_in_slice(&env, &input, input_pos, byte_len as usize) {
        Some(s) => s,
        None => {
            clear_pending_exception(&mut env);
            return JNI_FALSE;
        }
    };

    let mut bars_buf = [0.0f32; 64];
    if !dsp::compute_spectrum_bars(in_slice, channels as usize, sample_rate, &mut bars_buf[..bar_len]) {
        return JNI_FALSE;
    }
    if env.set_float_array_region(&out_bars, 0, &bars_buf[..bar_len]).is_err() {
        clear_pending_exception(&mut env);
        return JNI_FALSE;
    }
    JNI_TRUE
}
