import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Observable, Subject, of, timer } from 'rxjs';
import { Router } from '@angular/router';

@Component({
    selector: 'app-user',
    templateUrl: './user.component.html',
    styleUrls: ['./user.component.css']
})
export class UserComponent implements OnInit, OnDestroy {
    text: string = '';
    httpOptions = {
        headers: new HttpHeaders({ 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })
    };
    subscription?: any;

    constructor(private http: HttpClient, private router: Router) { }

    ngOnInit(): void {
        this.startTimer();
    }

    ngOnDestroy(): void {
        this.stopTimer();
    }

    // https://rxjs.dev/guide/observable
    startTimer() {
        this.subscription = new Observable(subscriber => {
            setTimeout(() => {
                subscriber.next(300);
            }, 10000);
        }).subscribe(() => this.logout());
    }

    stopTimer() {
        console.log('unsubscribe');
        this.subscription.unsubscribe();
    }

    logout(): void {
        this.http.post<void>('/logout', this.httpOptions) //
            .pipe(
                catchError(this.handleError<void>('logout'))
            ).subscribe(data => {
                console.log("LOGOUT USERS");
                location.href = '/login?logout';
            });
    }

    load(): void {
        this.http.get<string>('/api/users') //
            .pipe(
                catchError(this.handleError<string>('load users', '')))
            .subscribe(data => {
                this.stopTimer();
                this.startTimer();
                this.text = JSON.stringify(data)
            });
    }

    private handleError<T>(operation = 'operation', result?: T) {
        return (error: any): Observable<T> => {

            // TODO: send the error to remote logging infrastructure
            console.error(error); // log to console instead

            // TODO: better job of transforming error for user consumption
            console.log(`${operation} failed: ${error.message}`);

            // Let the app keep running by returning an empty result.
            return of(result as T);
        };
    }
}
