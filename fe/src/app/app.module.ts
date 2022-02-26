import { NgModule }      from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent }     from './app.component';
import { ExampleService }   from "./services/example.service";
import { HttpClientModule } from "@angular/common/http";
import { BASE_PATH }        from "@mycompany/myproject-api";
import { environment }      from "../environments/environment";

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
  ],
  providers: [ExampleService,
    {provide: BASE_PATH, useValue: environment.serverBase}],
  bootstrap: [AppComponent]
})
export class AppModule {
}
