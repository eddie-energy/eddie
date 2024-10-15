const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });

const table = initializeDataTable();
controlTheme();
controlRowExpansion(table);

function initializeDataTable() {
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
        render: function (data) {
          try {
            return COUNTRY_NAMES.of(data);
          } catch (e) {
            return data;
          }
        },
      },
      { data: "Region Connector" },
      { data: "PermissionId" },
      { data: "Start Date" },
      { data: "Status" },
      { data: "Action" },
    ],
  });
}

function controlTheme() {
  const root = document.documentElement;
  root.classList.add("dark");
  document
    .querySelector(".theme-controller")
    .addEventListener("change", function () {
      if (this.checked) {
        root.classList.remove("dark");
      } else {
        root.classList.add("dark");
      }
    });
}

function onTerminationButtonClick() {
  const permissionIdToTerminate = event.target.dataset.permissionId;
  const modal = document.getElementById("popup-modal");
  const modalQuestion = document.getElementById("modal-question");

  modal.dataset.permissionId = permissionIdToTerminate;
  modalQuestion.textContent = `Are you sure you want to terminate the permission ${permissionIdToTerminate}?`;
  modal.showModal();

  modal.addEventListener("close", () => {
    if (modal.returnValue === "confirm") {
      fetch(
        `/outbound-connectors/admin-console/terminate/${permissionIdToTerminate}`,
        {
          method: "POST",
        }
      )
        .then((response) => {
          if (response.ok) {
            console.log(
              `Termination initiated for permission ${permissionIdToTerminate}`
            );
            const success = document.querySelector(".alert-success");
            success.classList.remove("hidden");
            setTimeout(() => {
              success.classList.add("hidden");
            }, 5000);
          } else {
            console.error(
              `Failed to initiate termination for permission ${permissionIdToTerminate}`
            );
            const failure = document.querySelector(".alert-error");
            failure.classList.remove("hidden");
            setTimeout(() => {
              failure.classList.add("hidden");
            }, 5000);
          }
        })
        .catch((error) => console.error("Error:", error));
    }
    delete modal.dataset.permissionId;
  });
}

function controlRowExpansion(table) {
  table.on("click", "td.dt-control", function (e) {
    let tr = e.target.closest("tr");
    tr.classList.toggle("open"); // Toggle 'open' class on the row

    if (table.row(tr).child.isShown()) {
      table.row(tr).child.hide();
    } else {
      fetchAdditionalData(table.row(tr));
    }
  });
}

async function fetchAdditionalData(row) {
  try {
    const response = await fetch(
      `/outbound-connectors/admin-console/statusMessages/${row.data().PermissionId}`,
      { method: "GET" }
    );
    if (!response.ok) {
      throw new Error("Network response was not ok");
    }
    const additionalData = await response.json();
    row.child(formatTimeline(additionalData)).show();
  } catch (error) {
    console.log(error);
  }
}

function formatTimeline(rowPapertrailData) {
  const template = document.createElement("template");

  let html = /*HTML*/ `<ul class="timeline timeline-vertical">`;
  rowPapertrailData.forEach((statusMessage) => {
    html += /*HTML*/ `
      <li>
        <div class="timeline-end timeline-box">${statusMessage.status}</div>
        <div class="timeline-middle">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="var(--primary-color)" class="w-5 h-5">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 8 0 000 16" clip-rule="evenodd" />
            <text x="50%" y="50%" text-anchor="middle" fill="white" stroke-width="1px" dy=".3em">↑</text>
          </svg>
        </div>
        <div class="timeline-start">${statusMessage.startDate}</div>
        <hr/>
      </li>
    `;
  });
  html += `</ul>`;

  template.innerHTML = html;
  return template.content;
}
