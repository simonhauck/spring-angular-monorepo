import { NgModule }      from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent }     from './app.component';
import { ExampleService }   from "./services/example.service";
import { HttpClientModule } from "@angular/common/http";

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
  ],
  providers: [ExampleService],
  bootstrap: [AppComponent]
})
export class AppModule {
}
