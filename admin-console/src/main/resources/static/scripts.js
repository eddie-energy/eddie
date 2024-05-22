document.addEventListener("DOMContentLoaded", function () {
  const table = initializeDataTable();
  controlTheme();
  controlTerminationButtons();
  controlRowExpansion(table);
});


function initializeDataTable() {
    const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });
    return new DataTable("#statusMessageTable", {
      order: [],
      columns: [
        {
          className: "dt-control",
          orderable: false,
          data: null,
          defaultContent:
            '<svg xmlns="http://www.w3.org/2000/svg" fill="currentColor" viewBox="0 0 24 24" class="w-4 h-4 stroke-current"><path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"></path></svg>',
        },
        {
          data: "Country",
          render: function (data, type, row) {
            return COUNTRY_NAMES.of(data);
          },
        },
        { data: "DSO" },
        { data: "PermissionId" },
        { data: "Start Date" },
        { data: "Status" },
        { data: "Action" },
      ],
    });
}

function controlTheme() {
    const root = document.documentElement;
    root.classList.add('dark');
    document.querySelector('.theme-controller').addEventListener('change', function() {
        if (this.checked) {
            root.classList.remove('dark');
        }
        else {
            root.classList.add('dark');
        }
    });
}

function controlTerminationButtons() {

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
}

function controlRowExpansion(table) {
    table.on('click', 'td.dt-control', function (e) {
        let tr = e.target.closest('tr');
        let row = table.row(tr);
        let arrow = tr.querySelector('svg');

        if (row.child.isShown()) {
            row.child.hide();
            arrow.style.transform = 'rotate(0deg)';
        }
        else {
            fetchAdditionalData(row, arrow);
        }
    });
}

function fetchAdditionalData(row, arrow) {
    $.ajax({
        url: '/admin-console/statusMessages/' + row.data().PermissionId,
        type: 'GET',
        success: function(data) {
            row.child(formatTimeline(data)).show();
            arrow.style.transform = 'rotate(90deg)';
        },
        error: function(error) {
            console.log(error);
        }
    });
}

function formatTimeline(d) {
    let html = '<ul class="timeline timeline-vertical">'; // Change this to the color you want
    d.forEach(statusMessage => {
        html += '<li>';
        html += '<div class="timeline-end timeline-box">' + statusMessage.status + '</div>';
        html += '<div class="timeline-middle">';
        html += '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="var(--primary-color)" class="w-5 h-5">';
        html += '<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 8 0 000 16" clip-rule="evenodd" />'
        html += '<text x="50%" y="50%" text-anchor="middle" fill="white" stroke-width="1px" dy=".3em">↑</text>';
        html += '</svg>';
        html += '</div>';
        html += '<div class="timeline-start">' + statusMessage.startDate + '</div>';
        html += '<hr/>';
        html += '</li>';
    });
    html += '</ul>';
    return html;
}