import { Component, OnInit, ViewChild,ElementRef, OnDestroy } from '@angular/core';
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { WebSocketSubject } from 'rxjs/webSocket';
import { Subscription, timer } from 'rxjs';

@Component({
  selector: 'app-shell',
  standalone: false,
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.css']
})
export class ShellComponent implements OnInit, OnDestroy{
    @ViewChild('terminal', { static: true }) terminalDiv!: ElementRef;
    terminal!: Terminal;
    fitAddon!: FitAddon;
    socket$!: WebSocketSubject<string>;
    socketSubscription!: Subscription;


    ngOnInit(): void {
      this.initTerminal();
      this.connectWebSocket();
    }

initTerminal(): void {
  this.terminal = new Terminal({
    cursorBlink: true,
    theme: { background: '#1e1e1e', foreground: '#ffffff' },
    fontSize: 14,
    rows: 30,
    cols: 80
  });

  this.fitAddon = new FitAddon();
  this.terminal.loadAddon(this.fitAddon);
  this.terminal.open(this.terminalDiv.nativeElement);
  this.fitAddon.fit();

  this.terminal.onData((data) => {
    if (this.socket$) {
      this.socket$.next(data);  // Enviar el comando al backend
    }
  });

  this.terminal.writeln('Bienvenido al Shell Terminal');
  this.terminal.write('$ ');

  // Capturar la entrada del usuario (para escribir comandos)
  this.terminal.onKey((e) => {
    const char = e.key;

    if (char === '\r') {
      // Cuando presionas Enter, se envía el comando al backend
      this.socket$.next('\n');
      this.terminal.write('\r\n$ ');  // Muestra el prompt después de un comando
    } else if (char === '\u007f' || char === 'Backspace') {  // Backspace
      // Elimina un carácter de la terminal
      const currentText = this.terminal.getSelection();
      if (currentText.length > 0) {
        this.terminal.write('\b \b');  // Borra el último carácter en la terminal
      } else {
        // Si no hay texto seleccionado, solo mueve el cursor hacia atrás
        this.terminal.write('\b \b');
      }
    } else {
      this.terminal.write(char);  // Escribir el carácter en la terminal
    }
  });
}


  connectWebSocket(): void {
    this.socket$ = new WebSocketSubject('ws://localhost:8080/ws/shell');

    this.socketSubscription = this.socket$.subscribe({
      next: (message) => {
        if (this.terminal) {
          this.terminal.write('\r\n' + message + '\r\n$ '); // Muestra la salida con prompt
        }
      },
      error: (err) => console.error('WebSocket error:', err),
      complete: () => console.log('WebSocket cerrado')
    });
  }


  ngOnDestroy(): void {
    if (this.socketSubscription) {
      this.socketSubscription.unsubscribe();
    }
    if (this.socket$) {
      this.socket$.complete();
    }
  }
  }



