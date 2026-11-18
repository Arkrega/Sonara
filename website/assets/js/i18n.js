/**
 * SONARA — language switch.
 *
 * Pages are authored in English and window.SONARA_ID holds the Indonesian copy,
 * so Indonesian is the default presentation and English is a pure restore.
 * The choice is remembered per browser and can be flipped from the header, the
 * drawer, or any element carrying .lang-toggle.
 */

(function () {
  "use strict";

  var STORAGE_KEY = "sonara-lang";
  var DEFAULT_LANG = "id";
  var DICT = window.SONARA_ID || {};
  var META = window.SONARA_META || {};
  var ATTRS = ["aria-label", "placeholder", "title"];
  var SKIP_TAGS = /^(SCRIPT|STYLE|CODE|PRE|KBD|SAMP)$/;
  // Only real text containers may swallow inline markup; collapsing a span-based
  // layout element would destroy the structure, so those are handled per fragment.
  var TEXT_CONTAINERS = /^(P|LI|H1|H2|H3|H4|H5|H6|TD|TH|SUMMARY|FIGCAPTION|BLOCKQUOTE|DT|DD|CAPTION|LABEL|OPTION|ADDRESS|LEGEND)$/;

  var INLINE_TAGS = /^(A|STRONG|EM|B|I|SPAN|CODE|BR|SMALL|SUP|SUB|MARK|U|S|KBD|ABBR|TIME|SVG|PATH|CIRCLE|RECT|LINE|POLYLINE|POLYGON)$/;

  var blocks = [];
  var fragments = [];
  var attrTargets = [];

  function has(obj, key) {
    return Object.prototype.hasOwnProperty.call(obj, key);
  }

  function norm(value) {
    return String(value).replace(/\s+/g, " ").trim();
  }

  function isSkipped(el) {
    if (!el) return true;
    if (SKIP_TAGS.test(el.tagName)) return true;
    return !!(el.closest && el.closest("[data-i18n-ignore]"));
  }

  function onlyInlineChildren(el) {
    var kids = el.children;
    for (var i = 0; i < kids.length; i++) {
      if (!INLINE_TAGS.test(kids[i].tagName)) return false;
    }
    return true;
  }

  function stored() {
    try {
      var value = window.localStorage.getItem(STORAGE_KEY);
      if (value === "id" || value === "en") return value;
    } catch (e) {
      /* private mode */
    }
    return DEFAULT_LANG;
  }

  function collect() {
    var all = document.body.querySelectorAll("*");
    var done = [];
    var i;

    for (i = 0; i < all.length; i++) {
      var el = all[i];
      if (isSkipped(el)) continue;
      var hasKids = el.children.length > 0;
      if (hasKids && !(TEXT_CONTAINERS.test(el.tagName) && onlyInlineChildren(el))) continue;

      var insideDone = false;
      for (var d = 0; d < done.length; d++) {
        if (done[d] !== el && done[d].contains(el)) {
          insideDone = true;
          break;
        }
      }
      if (insideDone) continue;

      var key = norm(el.textContent);
      if (!key || !has(DICT, key)) continue;

      blocks.push({ el: el, en: el.innerHTML, id: DICT[key] });
      done.push(el);
    }

    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
    var node;
    while ((node = walker.nextNode())) {
      if (isSkipped(node.parentElement)) continue;
      var fragment = norm(node.nodeValue);
      if (!fragment || !has(DICT, fragment)) continue;
      fragments.push({ node: node, en: node.nodeValue, key: fragment });
    }

    var labelled = document.body.querySelectorAll("[data-id], [data-id-aria-label], [data-id-title], [data-id-placeholder]");
    for (i = 0; i < labelled.length; i++) {
      var node = labelled[i];
      if (isSkipped(node)) continue;
      if (node.hasAttribute("data-id") && node.hasAttribute("data-en")) {
        attrTargets.push({
          el: node,
          attr: null,
          en: node.getAttribute("data-en"),
          id: node.getAttribute("data-id"),
        });
      }
      for (var a = 0; a < ATTRS.length; a++) {
        var attr = ATTRS[a];
        var value = node.getAttribute("data-id-" + attr);
        if (value === null) continue;
        attrTargets.push({
          el: node,
          attr: attr,
          en: node.getAttribute("data-en-" + attr) || node.getAttribute(attr) || "",
          id: value,
        });
      }
    }
  }

  function applyMeta(lang) {
    var page = norm(window.location.pathname).replace(/\/$/, "") || "/";
    var entry = META[page];
    if (entry) {
      if (entry.title) document.title = entry.title[lang] || entry.title.en;
      var desc = document.querySelector('meta[name="description"]');
      if (desc && entry.description) {
        desc.setAttribute("content", entry.description[lang] || entry.description.en);
      }
    }
    document.documentElement.setAttribute("lang", lang);
  }

  function applyControls(lang) {
    var toggles = document.querySelectorAll(".lang-toggle");
    for (var i = 0; i < toggles.length; i++) {
      var toggle = toggles[i];
      var label = toggle.querySelector(".lang-toggle-label");
      if (label) label.textContent = lang.toUpperCase();
      toggle.setAttribute("aria-pressed", lang === "id" ? "false" : "true");
      toggle.setAttribute(
        "aria-label",
        lang === "id" ? "Ganti ke bahasa Inggris" : "Switch to Indonesian",
      );
    }
  }

  function render(lang) {
    var i;

    for (i = 0; i < blocks.length; i++) {
      var block = blocks[i];
      block.el.innerHTML = lang === "id" ? block.id : block.en;
    }

    for (i = 0; i < fragments.length; i++) {
      var fragment = fragments[i];
      if (lang === "id") {
        // keep the original padding so inline markup stays glued to the right words
        var lead = /^\s*/.exec(fragment.en)[0];
        var trail = /\s*$/.exec(fragment.en)[0];
        fragment.node.nodeValue = lead + DICT[fragment.key] + trail;
      } else {
        fragment.node.nodeValue = fragment.en;
      }
    }

    for (i = 0; i < attrTargets.length; i++) {
      var target = attrTargets[i];
      var value = lang === "id" ? target.id : target.en;
      if (target.attr) {
        target.el.setAttribute(target.attr, value);
      } else {
        target.el.textContent = value;
      }
    }

    applyMeta(lang);
    applyControls(lang);
  }

  function setLang(lang) {
    try {
      window.localStorage.setItem(STORAGE_KEY, lang);
    } catch (e) {
      /* ignore */
    }
    render(lang);
  }

  function init() {
    collect();
    render(stored());

    var toggles = document.querySelectorAll(".lang-toggle");
    for (var i = 0; i < toggles.length; i++) {
      toggles[i].addEventListener("click", function () {
        setLang(stored() === "id" ? "en" : "id");
      });
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }

  window.SONARA_LANG = { set: setLang, get: stored };
})();
