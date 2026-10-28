(function () {
  // Traffic presets on the requirement form.
  document.querySelectorAll('[data-fill]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var target = document.getElementById(btn.getAttribute('data-target'));
      if (target) { target.value = btn.getAttribute('data-fill'); target.focus(); }
    });
  });

  // Compare picker: allow 2-4 plans.
  var form = document.getElementById('compare-form');
  if (form) {
    var max = parseInt(form.getAttribute('data-max') || '4', 10);
    var boxes = Array.prototype.slice.call(form.querySelectorAll('input[name=ids]'));
    var button = document.getElementById('compare-btn');
    var count = document.getElementById('compare-count');
    var bar = document.getElementById('compare-bar');
    var update = function () {
      var checked = boxes.filter(function (b) { return b.checked; });
      boxes.forEach(function (b) { b.disabled = !b.checked && checked.length >= max; });
      if (count) { count.textContent = checked.length; }
      if (button) { button.disabled = checked.length < 2; }
      if (bar) { bar.hidden = checked.length === 0; }
    };
    boxes.forEach(function (b) { b.addEventListener('change', update); });
    update();
  }
})();
