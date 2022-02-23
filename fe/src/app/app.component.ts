import { Component }      from '@angular/core';
import { ExampleService } from "./services/example.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'fe';

  constructor(private exampleService: ExampleService) {
  }
}
