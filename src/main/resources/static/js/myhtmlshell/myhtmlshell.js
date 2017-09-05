function onopen(term, socket) {
  term.attach(socket);
  term._initialized = true;
}

function runFakeTerminal(term) {
  if (term._initialized) {
    return;
  }

  term._initialized = true;

  var shellprompt = '$ ';

  term.prompt = function () {
    term.write('\r\n' + shellprompt);
  };

  term.writeln('Welcome to MyHtmlShell');
  term.writeln('');
  term.prompt();

  term.on('key', function (key, ev) {
    var printable = (
      !ev.altKey && !ev.altGraphKey && !ev.ctrlKey && !ev.metaKey
    );

    if (ev.keyCode == 13) {
      term.prompt();
    } else if (ev.keyCode == 8) {
     // Do not delete the prompt
      if (term.x > 2) {
        term.write('\b \b');
      }
    } else if (printable) {
      term.write(key);
    }
  });

  term.on('paste', function (data, ev) {
    term.write(data);
  });
}

var socket = null;

(function () {

    var protocol = (location.protocol === 'https:') ? 'wss://' : 'ws://';
    var socketURL = protocol + location.hostname + ((location.port) ? (':' + location.port) : '') + '/ws-myhtmlshell';


    socket = new WebSocket(socketURL);
    socket.onmessage = function(data){
        // data.data

    };


    var terminalContainer = document.getElementById('terminal-container');

    while (terminalContainer.children.length) {
        terminalContainer.removeChild(terminalContainer.children[0]);
    }
    var term = new Terminal({
        cursorBlink : true,
        scrollback : 10,
        tabStopWidth : 10
    });
    term.open(terminalContainer);
    term.fit();

    var initialGeometry = term.proposeGeometry();

    socket.onopen = onopen(term, socket);
    socket.onclose = runFakeTerminal(term);
    socket.onerror = runFakeTerminal(term);

})();




