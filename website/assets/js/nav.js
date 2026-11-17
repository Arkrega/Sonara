/**
 * SONARA — shared navigation script.
 * Handles the mobile drawer and active-page detection.
 */

(function () {
  "use strict";

  // ---------------------------------------------------------------- mobile navigation
  var toggle = document.getElementById("nav-toggle");
  var drawer = document.getElementById("nav-drawer");
  var backdrop = document.getElementById("nav-backdrop");

  function openDrawer() {
    if (!drawer) return;
    drawer.classList.add("open");
    toggle.setAttribute("aria-expanded", "true");
    document.body.style.overflow = "hidden";
  }

  function closeDrawer() {
    if (!drawer) return;
    drawer.classList.remove("open");
    toggle.setAttribute("aria-expanded", "false");
    document.body.style.overflow = "";
  }

  if (toggle) {
    toggle.addEventListener("click", openDrawer);
  }

  if (backdrop) {
    backdrop.addEventListener("click", closeDrawer);
  }

  if (drawer) {
    drawer.addEventListener(
      "click",
      function (e) {
        if (e.target.tagName === "A") {
          closeDrawer();
        }
      },
      false,
    );
  }

  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" && drawer && drawer.classList.contains("open")) {
      closeDrawer();
    }
  });

  // ---------------------------------------------------------------- active page indicator
  var path = window.location.pathname;
  var urlPath = path === "/" ? "" : path.replace(/\/$/, "");

  var links = document.querySelectorAll(".nav-link");
  links.forEach(function (link) {
    link.classList.remove("active");
    var href = link.getAttribute("href");
    if (!href) return;
    var linkPath = href.replace(/\/$/, "");
    if (linkPath === urlPath || href === "/" && urlPath === "") {
      link.classList.add("active");
    }
  });
})();
