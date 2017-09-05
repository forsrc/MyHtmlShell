var socket = null;

(function() {

    var protocol = (location.protocol === 'https:') ? 'wss://' : 'ws://';
    var socketURL = protocol + location.hostname
            + ((location.port) ? (':' + location.port) : '')
            + '/ws-myhtmlshell';

    socket = new WebSocket(socketURL);
    socket.onmessage = function(data){
        term.write(data.data);
        term.prompt();
    };

    var terminalContainer = document.getElementById('terminal-container');

    while (terminalContainer.children.length) {
        terminalContainer.removeChild(terminalContainer.children[0]);
    }
    var term = new Terminal({
        cursorBlink : true,
    // scrollback : 10,
    // tabStopWidth : 10
    });
    term.open(terminalContainer);
    term.fit();

    var initialGeometry = term.proposeGeometry();
    term.resize(document.body.clientWidth, document.body.clientHeight);
    socket.onopen = function() {
        term.writeln("MyHtmlShell\r\n");
        term.prompt();
    };
    socket.onclose = function() {
        socket.close();
    };
    socket.onerror = function() {
        
    };

    var keys = "";
    term
            .on('key',
                    function(key, ev) {
                        //alert(ev.keyCode);
                        var printable = (!ev.altKey && !ev.altGraphKey
                                && !ev.ctrlKey && !ev.metaKey);

                        if (ev.keyCode == 13) {
                            socket.send(keys);
                            keys = "";
                            term.prompt();
                        } else if (ev.keyCode == 8) {
                            // Do not delete the prompt
                            if (keys.length > 0) {
                                term.write('\b \b');
                                keys = keys.substring(0, keys.length - 1);
                            }
                        } else if (printable) {
                            term.write(key);
                            keys += key;
                        }
                    });

    term.on('paste', function(data, ev) {
        term.write(data);
    });

})();
