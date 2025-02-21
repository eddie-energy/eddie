# Embed the EDDIE Button in Angular

Angular [works well](https://custom-elements-everywhere.com/#angular) with custom elements, so the EDDIE Button can be integrated with ease.
General information on how to configure the EDDIE Button is found [here](eddie-button.md).

## Importing the custom element

The EDDIE Button is provided by an endpoint of the EDDIE framework.
It is not available as a npm package at build time, and should therefore be imported as an external script.
This can be done in your `index.html` file:

```html
<script type="module" src="${eddieUrl}/lib/eddie-components.js"></script>
```

Angular components embedding the EDDIE button have to include the [custom elements schema](https://angular.dev/guide/components/advanced-configuration#custom-element-schemas).

```ts {2-3}
@Component({
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<eddie-connect-button>...</eddie-connect-button>`,
})
export class EddieButtonComponent {}
```

## Event binding

Angular can bind to the [custom events](eddie-button.md#request-status-and-interaction-events) emitted by the EDDIE Button using [event binding](https://v17.angular.io/guide/event-binding).

```ts {1,7-10,15}
// noinspection AngularUndefinedBinding
@Component({
  selector: "eddie-button-component",
  standalone: true,
  template: /* HTML */ `
    <eddie-connect-button
      (eddie-dialog-open)="dialogOpen()"
      (eddie-dialog-close)="dialogClose()"
      (eddie-request-created)="statusCreated()"
      (eddie-request-status)="statusChanged($event)"
      connection-id="1"
      data-need-id="9bd0668f-cc19-40a8-99db-dc2cb2802b17"
    ></eddie-connect-button>
  `,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class EddieButtonComponent {
  dialogOpen() {
    console.log("Dialog Open");
  }

  dialogClose() {
    console.log("Dialog Close");
  }

  statusCreated() {
    console.log("Status Created");
  }

  statusChanged(event: Event) {
    console.log((event as CustomEvent).detail);
  }
}
```

## Event listeners

You may also choose to add an event listener using `addEventListener`.

```html
<eddie-connect-button #eddieButton></eddie-connect-button>
```

```ts
@Component({
  /* ... */
})
export class EddieButtonComponent implements AfterViewInit {
  @ViewChild("eddieButton") eddieButton!: ElementRef;

  ngAfterViewInit() {
    this.eddieButton.nativeElement.addEventListener(
      "eddie-dialog-open",
      this.dialogOpen
    );
  }
}
```

## Callback attributes

Angular does not allow the use of [callback attributes](eddie-button.md#callback-attributes) like `onopen` for security reasons.
The following will show a warning, and `dialogOpen()` will not run when the dialog is opened.

```html
<eddie-connect-button [attr.onopen]="dialogOpen()"></eddie-connect-button>
```
