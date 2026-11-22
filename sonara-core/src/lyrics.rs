fn is_line_terminator(c: char) -> bool {
    matches!(c, '\n' | '\r' | '\u{0085}' | '\u{2028}' | '\u{2029}')
}

pub fn clean_title_for_search(title: &str) -> String {
    let mut out = String::with_capacity(title.len());
    let mut i = 0usize;
    let bytes = title.as_bytes();

    while i < bytes.len() {
        let mut ws_end = i;
        while ws_end < title.len() {
            let ch = title[ws_end..].chars().next().unwrap();
            if ch.is_whitespace() {
                ws_end += ch.len_utf8();
            } else {
                break;
            }
        }

        if let Some(open_ch) = title[ws_end..].chars().next() {
            if open_ch == '(' || open_ch == '[' {
                let search_start = ws_end + open_ch.len_utf8();
                let mut scan = search_start;
                let mut found_close: Option<usize> = None;
                while scan < title.len() {
                    let c = title[scan..].chars().next().unwrap();
                    if is_line_terminator(c) {
                        break;
                    }
                    if c == ')' || c == ']' {
                        found_close = Some(scan + c.len_utf8());
                        break;
                    }
                    scan += c.len_utf8();
                }
                if let Some(next_i) = found_close {
                    i = next_i;
                    continue;
                }
            }
        }

        let ch = title[i..].chars().next().unwrap();
        out.push(ch);
        i += ch.len_utf8();
    }

    out.trim().to_string()
}

fn split_lines(s: &str) -> Vec<&str> {
    let mut lines = Vec::new();
    let bytes = s.as_bytes();
    let mut start = 0usize;
    let mut i = 0usize;
    while i < bytes.len() {
        if bytes[i] == b'\r' {
            lines.push(&s[start..i]);
            if i + 1 < bytes.len() && bytes[i + 1] == b'\n' {
                i += 2;
            } else {
                i += 1;
            }
            start = i;
        } else if bytes[i] == b'\n' {
            lines.push(&s[start..i]);
            i += 1;
            start = i;
        } else {
            i += 1;
        }
    }
    lines.push(&s[start..]);
    lines
}

fn strip_leading_timestamp(s: &str) -> Option<&str> {
    let b = s.as_bytes();
    if b.len() < 10 {
        return None;
    }
    if b[0] != b'[' || !b[1].is_ascii_digit() || !b[2].is_ascii_digit() || b[3] != b':' {
        return None;
    }
    if !b[4].is_ascii_digit() || !b[5].is_ascii_digit() || b[6] != b'.' {
        return None;
    }
    if !b[7].is_ascii_digit() || !b[8].is_ascii_digit() {
        return None;
    }
    if b[9] == b']' {
        return Some(&s[10..]);
    }
    if b.len() >= 11 && b[9].is_ascii_digit() && b[10] == b']' {
        return Some(&s[11..]);
    }
    None
}

fn strip_leading_agent(s: &str) -> Option<&str> {
    let rest = s.strip_prefix("{agent:")?;
    let end = rest.find('}')?;
    if end == 0 {
        return None;
    }
    Some(&rest[end + 1..])
}

fn strip_leading_bg_tag(s: &str) -> Option<&str> {
    s.strip_prefix("{bg}")
}

fn strip_leading_bg_bracket(s: &str) -> Option<&str> {
    let rest = s.strip_prefix("[bg:")?;
    let last_close = rest.rfind(']')?;
    Some(&rest[last_close + 1..])
}

fn strip_leading_version(s: &str) -> Option<&str> {
    let rest = s.strip_prefix('v')?;
    let b = rest.as_bytes();
    let mut digits = 0usize;
    while digits < b.len() && b[digits].is_ascii_digit() {
        digits += 1;
    }
    if digits == 0 || digits >= b.len() || b[digits] != b':' {
        return None;
    }
    Some(&rest[digits + 1..])
}

pub fn filter_lyrics_credit_lines(lyrics: &str) -> String {
    let mut kept = Vec::new();
    for line in split_lines(lyrics) {
        let mut text = line.trim();
        loop {
            let prev_len = text.len();
            if let Some(s) = strip_leading_timestamp(text) {
                text = s;
            }
            if let Some(s) = strip_leading_agent(text) {
                text = s;
            }
            if let Some(s) = strip_leading_bg_tag(text) {
                text = s;
            }
            if let Some(s) = strip_leading_bg_bracket(text) {
                text = s;
            }
            if let Some(s) = strip_leading_version(text) {
                text = s;
            }
            text = text.trim();
            if text.len() >= prev_len {
                break;
            }
        }

        let lower = text.to_lowercase();
        let utf16_len = lower.encode_utf16().count();
        let is_credit = lower.starts_with("synced by")
            || lower.starts_with("lyrics by")
            || lower.starts_with("music by")
            || lower.starts_with("arranged by")
            || (lower.starts_with('[')
                && lower.ends_with(']')
                && utf16_len < 40
                && lower.contains("synced by"));

        if !is_credit {
            kept.push(line);
        }
    }
    kept.join("\n")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn clean_title_strips_brackets_and_parens() {
        assert_eq!(
            clean_title_for_search("Bohemian Rhapsody (Remastered 2011) [Official Audio]"),
            "Bohemian Rhapsody"
        );
        assert_eq!(
            clean_title_for_search("  Song Title (feat. Someone) - Live  "),
            "Song Title - Live"
        );
        assert_eq!(clean_title_for_search("Unclosed (Title"), "Unclosed (Title");
        assert_eq!(clean_title_for_search("Mixed [a) Normal"), "Mixed Normal");
    }

    #[test]
    fn filter_credits_removes_credit_lines_and_keeps_lyrics() {
        let input = "[00:00.00]Synced by John\n[00:01.00]{agent:v1}v1: Lyrics by Someone\n[00:02.50]{bg}[bg: synced by someone]\n[00:05.00]First real lyric line\n[00:09.123]v2: Second real lyric line\n[Synced by Tester]";
        let expected = "[00:02.50]{bg}[bg: synced by someone]\n[00:05.00]First real lyric line\n[00:09.123]v2: Second real lyric line";
        assert_eq!(filter_lyrics_credit_lines(input), expected);

        let input2 = "[00:00.000]v1: Music by Artist\n[00:01.000]Arranged by Producer\n[00:02.000][Synced by User]\n[00:03.000]Actual line\r\n";
        let expected2 = "[00:03.000]Actual line\n";
        assert_eq!(filter_lyrics_credit_lines(input2), expected2);
    }
}
