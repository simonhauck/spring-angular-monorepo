import { Injectable } from '@angular/core';
import {ExampleControllerService} from "@mycompany/myproject-api";

@Injectable({
  providedIn: 'root'
})
export class ExampleService {

  constructor(private api: ExampleControllerService) {
    console.log("Test")
    api.getHelloWorld().subscribe(value => console.log("Received: " + value.response));
  }
}
