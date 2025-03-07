import { Component, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { WebSocketSubject } from 'rxjs/webSocket';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-shell',
  standalone: false,
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.css']
})
export class ShellComponent implements OnInit, OnDestroy {
  @ViewChild('terminal', { static: true }) terminalDiv!: ElementRef;
  terminal!: Terminal;
  fitAddon!: FitAddon;
  socket$!: WebSocketSubject<string>;
  socketSubscription!: Subscription;
  inputBuffer: string = "";

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

    this.terminal.writeln('Bienvenido al Shell Terminal');
    this.showPrompt();

    this.terminal.onKey((e) => {
      const char = e.key;

      if (char === '\r') {  // Enter
        this.terminal.write('\r\n');
        this.socket$.next(this.inputBuffer);  // Enviar al backend
        this.inputBuffer = "";
      } else if (char === '\u007f' || char === 'Backspace') {  // Backspace
        if (this.inputBuffer.length > 0) {
          this.inputBuffer = this.inputBuffer.slice(0, -1);
          this.terminal.write('\b \b');
        }
      } else {
        this.inputBuffer += char;
        this.terminal.write(char);
      }
    });
  }

  showPrompt(): void {
    this.terminal.write('$ ');
  }

  connectWebSocket(): void {
    this.socket$ = new WebSocketSubject({
      url: 'ws://localhost:8080/ws/shell',
      serializer: (msg: string) => msg,
      deserializer: (event: MessageEvent) => event.data.toString(),
      openObserver: {
        next: () => {
          console.log("WebSocket conectado.");
          this.terminal.writeln('Conectado al shell.');
          this.showPrompt();
        }
      },
      closeObserver: {
        next: () => {
          console.log("WebSocket cerrado. Intentando reconectar...");
          this.terminal.writeln('\nWebSocket cerrado. Reconectando...');
          setTimeout(() => this.connectWebSocket(), 3000);
        }
      }
    });

    this.socketSubscription = this.socket$.subscribe({
      next: (message) => {
        console.log("Salida del shell:", message);
        this.terminal.writeln(message);
        this.showPrompt();
      },
      error: (err) => {
        console.error('WebSocket error:', err);
        setTimeout(() => this.connectWebSocket(), 3000);
      }
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
