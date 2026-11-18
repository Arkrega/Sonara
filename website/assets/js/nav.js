/**
 * SONARA — shared navigation script.
 * Highlights the current page in both the desktop header and the mobile strip.
 */

(function () {
  "use strict";

  var path = window.location.pathname.replace(/\/+$/, "");
  var current = path === "" ? "/" : path;

  var links = document.querySelectorAll(".nav-link");
  links.forEach(function (link) {
    var href = (link.getAttribute("href") || "").replace(/\/+$/, "");
    if (href === "") href = "/";
    link.classList.toggle("active", href === current);
    if (href === current) {
      link.setAttribute("aria-current", "page");
    }
  });

  // Keep the active chip in view inside the scrollable mobile strip.
  var strip = document.querySelector(".mobile-nav-scroll");
  var activeChip = strip && strip.querySelector(".nav-link.active");
  if (strip && activeChip) {
    var offset = activeChip.offsetLeft - (strip.clientWidth - activeChip.offsetWidth) / 2;
    strip.scrollLeft = Math.max(0, offset);
  }
})();
