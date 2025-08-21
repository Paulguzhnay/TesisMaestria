import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';

import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, NgClass } from '@angular/common';


@Component({
  selector: 'app-shell-exec',
  standalone: false,
  templateUrl: './shell-exec.component.html',
  styleUrl: './shell-exec.component.css'
})

export class ShellExecComponent implements OnInit {
  @ViewChild('terminal', { static: true }) terminalContainer!: ElementRef;
  @ViewChild('termBox', { static: true }) termBox!: ElementRef;

  terminal!: Terminal;
  fitAddon!: FitAddon;
  containerName = '';
  inputBuffer = '';
  history: string[] = [];
  histIdx = -1;
  ws?: WebSocket;
  currentPwd = '';

  constructor(private route: ActivatedRoute) { }

  ngOnInit(): void {
    this.initTerminal();

    const parentRoute = this.route.parent;
    if (!parentRoute) {
      this.terminal.writeln('❌ No se pudo acceder a la ruta padre.');
      this.terminal.scrollToBottom();
      return;
    }


    parentRoute.paramMap.subscribe(params => {
      const instanceName = params.get('instanceName') || '';
      const category = this.route.snapshot.queryParamMap.get('category') || '';
      this.containerName = `odoo_instance_${category}_${instanceName}`;

      this.terminal.writeln(`\x1b[1;32mShell interactivo en: ${this.containerName}\x1b[0m`);
      this.terminal.scrollToBottom();
      this.connectSocket();
    });
  }
  private scrollToBottomBoth() {
    try { this.terminal.scrollToBottom(); } catch { }
    try {
      const el = this.termBox?.nativeElement as HTMLElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch { }
  }

  connectSocket() {
    if (!this.containerName) return;

    // Fuerza host backend en dev (mueve a environment.ts en prod)
    const backendHost = 'localhost:8080';
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${proto}://${backendHost}/ws/shell?container=${encodeURIComponent(this.containerName)}`;

    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      this.terminal.writeln('\x1b[32m[WS abierto]\x1b[0m');
      this.terminal.scrollToBottom();
      this.terminal.focus();
    };

    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data);
        switch (msg.type) {
          case 'stdout':
            this.terminal.write(msg.data);
            this.terminal.scrollToBottom();
            break;
          case 'stderr':
            this.terminal.write(`\x1b[31m${msg.data}\x1b[0m`);
            this.terminal.scrollToBottom();
            break;
          case 'pwd':
            this.currentPwd = msg.data || this.currentPwd;
            this.printPrompt();
            break;
          case 'status':
            this.terminal.writeln(`[${msg.data}]`);
            this.terminal.scrollToBottom();
            break;
          case 'error':
            this.terminal.writeln(`\x1b[31m[ERROR] ${msg.data}\x1b[0m`);
            this.terminal.scrollToBottom();
            break;
        }
      } catch {
        this.terminal.write(ev.data);
        this.terminal.scrollToBottom();
      }
      this.scrollToBottomBoth();
    };

    this.ws.onerror = () => {
      this.terminal.writeln('\x1b[31m[WS error]\x1b[0m');
      this.terminal.scrollToBottom();
    };

    this.ws.onclose = (e) => {
      this.terminal.writeln(`\r\n\x1b[33m[WS cerrado code=${e.code} reason=${e.reason}]\x1b[0m`);
      this.terminal.scrollToBottom();
    };
  }

  initTerminal(): void {
    this.terminal = new Terminal({
      theme: { background: '#000000', foreground: '#00FF00' },
      fontSize: 14,
      cursorBlink: true,
      disableStdin: false,
      convertEol: true,
      scrollback: 10000,
      fontFamily: 'ui-monospace,SFMono-Regular,Menlo,Consolas,monospace'
    });

    this.fitAddon = new FitAddon();
    this.terminal.loadAddon(this.fitAddon);
    this.terminal.open(this.terminalContainer.nativeElement);
    this.fitAddon.fit();

    new ResizeObserver(() => this.fitAddon.fit())
      .observe(this.termBox.nativeElement);

    // Si el usuario hace click en el box, vuelve a enfocar el terminal
    (this.termBox.nativeElement as HTMLElement).addEventListener('click', () => this.terminal.focus());


    this.terminal.onData((data) => {
      const code = data.charCodeAt(0);
      switch (code) {
        case 13: { // Enter
          const command = this.inputBuffer;
          this.terminal.write('\r\n');
          if (command.trim().length > 0) {
            this.history.unshift(command);
            this.histIdx = -1;
            this.sendCommand(command);
          } else {
            this.sendCommand(':');
          }
          this.inputBuffer = '';
          this.scrollToBottomBoth();
          break;
        }
        case 127: { // Backspace
          if (this.inputBuffer.length > 0) {
            this.inputBuffer = this.inputBuffer.slice(0, -1);
            this.terminal.write('\b \b');
            this.scrollToBottomBoth();
          }
          break;
        }
        default: {
          // Historial ↑/↓
          if (data === '\u001b[A') { if (this.history.length) { this.histIdx = Math.min(this.histIdx + 1, this.history.length - 1); this.replaceLine(this.history[this.histIdx]); } return; }
          if (data === '\u001b[B') { if (this.histIdx > 0) { this.histIdx--; this.replaceLine(this.history[this.histIdx]); } else { this.histIdx = -1; this.replaceLine(''); } return; }

          // Texto normal
          this.inputBuffer += data;
          this.terminal.write(data);
          this.scrollToBottomBoth();
          break;
        }
      }
    });
  }

  printPrompt() {
    const path = this.currentPwd || '~';
    this.terminal.write(`\r\n\uE0B0 ${path} $ `);
    this.scrollToBottomBoth();
    this.terminal.focus();               // ← foco tras prompt
  }

  replaceLine(text: string) {
    const toErase = this.inputBuffer.length;
    for (let i = 0; i < toErase; i++) this.terminal.write('\b \b');
    this.inputBuffer = text;
    this.terminal.write(text);
    this.scrollToBottomBoth();
  }

  sendCommand(cmd: string) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      this.terminal.writeln('❌ WS no conectado');
      this.terminal.scrollToBottom();
      return;
    }
    this.ws.send(JSON.stringify({ type: 'cmd', data: cmd }));
  }
}