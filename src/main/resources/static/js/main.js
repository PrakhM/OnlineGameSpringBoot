
document.addEventListener('keydown', function(event) {
    if (event.key === 'ArrowUp') {
        move(0);
    } else if (event.key === 'ArrowDown') {
        move(2);
    } else if(event.key === 'ArrowLeft'){
        move(1);
    } else if(event.key === 'ArrowRight'){
        move(3);
    }
});

function onConnected() {
    stompClient.subscribe('/topic/updates', updateMap);

    stompClient.send("/app/player.addPlayer",
        {},
        JSON.stringify({tag: username, coordinate: [0, 0]});
    )
}

function move(dir) {
        stompClient.send("/app/player.movePlayer", {}, dir);
    }
}

function updateMap(payload)
{

}
