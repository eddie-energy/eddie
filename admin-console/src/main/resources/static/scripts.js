(function(){
  new DataTable("#statusMessageTable" ,{ "order": [] });

  const body = document.querySelector('body');
  document.addEventListener('DOMContentLoaded', body.classList.add('dark'));
  document.querySelector('.theme-controller').addEventListener('change', function() {
    if (this.checked) {
        body.classList.remove('dark');
    }
    else {
        body.classList.add('dark');
    }
  });

  const modal = document.getElementById('popup-modal');
  const modalQuestion = document.getElementById('modal-question');

  const terminatePermissionButtons = document.querySelectorAll('.js-terminate-permission');

  for (const button of terminatePermissionButtons) {
    button.addEventListener("click", () => {
      const permissionIdToTerminate = button.dataset.permissionId;

      modal.dataset.permissionId = permissionIdToTerminate;

      modalQuestion.textContent = `Are you sure you want to terminate the permission ${permissionIdToTerminate}?`;

      modal.showModal();
    });
  }

  modal.addEventListener("close", () => {
    if (modal.returnValue === "confirm") {
      // TODO: Send termination request
      console.log(`Terminating permission ${modal.dataset.permissionId}`);
    }

    delete modal.dataset.permissionId;
  });
})();