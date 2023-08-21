import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UserComponent } from './user/user.component';
import { HelloComponent } from './hello/hello.component';

const routes: Routes = [
    { path: 'user', component: UserComponent, },
    { path: 'hello', component: HelloComponent, },
    { path: '', redirectTo: 'user', pathMatch: 'full' },
];

@NgModule({
imports: [RouterModule.forRoot(routes)],
exports: [RouterModule]
})
export class AppRoutingModule { }