pub mod lyrics;
pub mod shazam;

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::jstring;
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
