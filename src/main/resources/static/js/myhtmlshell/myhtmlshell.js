var socket = null;
var term = null;

(function() {

    term = new Terminal({
        cursorBlink : true,
        scrollback : 1000,
        tabStopWidth : 4
    });

    var protocol = (location.protocol === 'https:') ? 'wss://' : 'ws://';
    var socketURL = protocol + location.hostname
            + ((location.port) ? (':' + location.port) : '')
            + '/ws-myhtmlshell';

    socket = new WebSocket(socketURL);
    socket.onopen = function(evt) {
        //alert("open");
        console.log('Connection open ...');
    };
    socket.onclose = function(evt) {
        console.log('Connection closed');
    };
    socket.onerror = function(evt) {
        console.log('onerror');
    };
    socket.onmessage = function(data){
        console.log(data.data);
        term.write(data.data);
        //term.prompt();
    };

    //socket.connect();

    var terminalContainer = document.getElementById('terminal-container');

    while (terminalContainer.children.length) {
        terminalContainer.removeChild(terminalContainer.children[0]);
    }

    term.open(terminalContainer);
    term.fit();

    term.resize(document.body.clientWidth, document.body.clientHeight);
    socket.onopen = function() {
        term.writeln("MyHtmlShell\r\n");
        //term.prompt();
    };


    term.getInput = function () {
        var _this = term;
        var span = _this.element.querySelector('.terminal-cursor').parentNode.childNodes[0];
        var text = span.innerHTML;

        return text.replace(/(<span class="xterm\-(wide|normal)\-char">)|(<\/span>)/g,'');
    }

    term.on('data', function(data) {
        term.write(data);
    });
            
    term.on('key',
            function(key, ev) {
                //alert(key);
                console.log(ev);
                var printable = (!ev.altKey && !ev.altGraphKey
                        && !ev.ctrlKey && !ev.metaKey);
                var input = term.getInput();

                if (ev.keyCode == 13) {
                    socket.send(input);
                    //term.prompt();
                } else if (ev.keyCode == 8) {
                    // Do not delete the prompt
                    if (input.length > 1) {
                        term.write('\b \b');
                    }
                } else if (printable) {
                    //term.write(key);
                }
            });

    term.on('paste', function(data, ev) {
        term.write(data);
    });

})();
