$(document).ready(function() {
  const table = $('#statusMessageTable').DataTable({ "order": [] });

  function styleCurrentButton() {
    $('.paginate_button.current').attr('id', 'currentButton');
  }
  styleCurrentButton();
  table.on('draw.dt', styleCurrentButton);

  document.querySelector('.theme-controller').addEventListener('change', function() {
    document.body.classList.toggle('light', this.checked);
  });

  const modal = document.getElementById('popup-modal');
  const btns = document.querySelectorAll('[data-modal-target="popup-modal"]');
  const closeBtns = document.querySelectorAll('[data-modal-hide="popup-modal"]');
  let permissionIdToTerminate;

  btns.forEach(function(btn) {
    btn.onclick = function() {
      modal.classList.remove('hidden');
      permissionIdToTerminate = this.getAttribute('data-permission-id');
      document.getElementById('modal-question').textContent = 'Are you sure you want to terminate the permission ' + permissionIdToTerminate + '?';
    }
  });

  closeBtns.forEach(function(btn) {
    btn.onclick = function() {
      modal.classList.add('hidden');
      permissionIdToTerminate = null;
    }
  });
  const yesButton = document.querySelector('#popup-modal button.text-white');
  if (yesButton) {
    yesButton.onclick = function() {
      // Use permissionIdToTerminate to terminate the correct permission
      // ...
      modal.classList.add('hidden');
    }
  }
});