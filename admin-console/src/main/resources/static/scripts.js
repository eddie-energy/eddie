// you can remove the self-calling if you use a build tool
// kept it, so it is easier for you to spot my changes
(function(){
  new DataTable("#statusMessageTable" ,{ "order": [] });

  function styleCurrentButton() {
    document.querySelector(".paginate_button.current")?.setAttribute("id", "currentButton");
  }
  styleCurrentButton();

  document.querySelector("#statusMessageTable").addEventListener("draw.dt", styleCurrentButton);

  document.querySelector('.theme-controller').addEventListener('change', function() {
    document.body.classList.toggle('light', this.checked);
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
