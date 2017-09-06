var socket = null;
var term = null;

(function() {

    var protocol = (location.protocol === 'https:') ? 'wss://' : 'ws://';
    var socketURL = protocol + location.hostname
            + ((location.port) ? (':' + location.port) : '')
            + '/ws-myhtmlshell';

    socket = new WebSocket(socketURL);
    socket.onmessage = function(data){
        term.write(data.data);
        //term.prompt();
    };

    var terminalContainer = document.getElementById('terminal-container');

    while (terminalContainer.children.length) {
        terminalContainer.removeChild(terminalContainer.children[0]);
    }
    term = new Terminal({
        cursorBlink : true,
        scrollback : 1000,
        tabStopWidth : 4
    });
    term.open(terminalContainer);
    term.fit();

    term.resize(document.body.clientWidth, document.body.clientHeight);
    socket.onopen = function() {
        term.writeln("MyHtmlShell\r\n");
        //term.prompt();
    };
    socket.onclose = function() {
        socket.close();
    };
    socket.onerror = function() {
        
    };

    term.getInput = function () {
        var _this = term;
        var span = _this.element.querySelector('.terminal-cursor').parentNode.childNodes[0];
        var text = span.innerHTML;

        return text.replace(/(<span class="xterm-wide-char">)|(<\/span>)/g,'');
    }

    term
            .on('key',
                    function(key, ev) {
                        //alert(key);
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
                            term.write(key);
                        }
                    });

    term.on('paste', function(data, ev) {
        term.write(data);
    });

})();
