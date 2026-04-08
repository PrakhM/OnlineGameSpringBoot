const TILE = 32;
const MAP_W = 20;
const MAP_H = 15;
const SCALE = 1.5;
const STILE = TILE * SCALE;

const playerImg = new Image();
playerImg.src = '/images/player.png';
const playerLinesImg = new Image();
playerLinesImg.src = '/images/playerLines.png';

const TILE_IMGS = {
    0:   new Image(),
    1:   new Image(),
    2:   new Image(),
    3:   new Image(),
    4:   new Image(),
    10:  new Image(),
    20:  new Image(),
    30:  new Image(),
    100: new Image(),
    110: new Image(),
};
TILE_IMGS[0].src   = '/images/grass.png';
TILE_IMGS[1].src   = '/images/tree.png';
TILE_IMGS[2].src   = '/images/water.png';
TILE_IMGS[3].src   = '/images/breeGrass.png';
TILE_IMGS[4].src   = '/images/breeTree.png';
TILE_IMGS[10].src  = '/images/portal.png';
TILE_IMGS[20].src  = '/images/portal.png';
TILE_IMGS[30].src  = '/images/portal.png';
TILE_IMGS[100].src = '/images/archeryTarget.png';
TILE_IMGS[110].src = '/images/bow.png';

const MAPS = [
    [
        [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
        [1,0,0,0,0,0,1,0,0,0,0,100,100,0,0,0,0,0,0,1],
        [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
        [1,0,0,1,1,0,0,0,0,1,1,0,0,0,1,0,0,0,0,1],
        [1,0,0,1,1,0,0,0,0,1,1,110,110,0,1,0,0,0,0,1],
        [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,1],
        [1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,2,0,1],
        [1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,2,0,1],
        [1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1],
        [1,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1],
        [1,0,0,1,0,0,0,0,0,0,0,0,0,1,1,1,0,0,0,1],
        [1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0,20],
        [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
        [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
        [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
    ],
    [
        [4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,30],
        [4,4,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,30],
        [4,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,3,30],
        [4,4,4,4,4,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,4,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [10,3,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
        [4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4],
    ],
];

const canvas = document.getElementById('gameCanvas');
canvas.width  = MAP_W * STILE;
canvas.height = MAP_H * STILE;
const ctx = canvas.getContext('2d');
ctx.imageSmoothingEnabled = false;

const offscreen = document.createElement('canvas');
offscreen.width  = STILE;
offscreen.height = STILE;
const off = offscreen.getContext('2d');
off.imageSmoothingEnabled = false;

// state
let stompClient = null;
let username = null;
let players = {};
let wantsToDoActivity = false;

function drawMap() {
    const me = players[username];
    const mapIndex = me ? me.location : 0;
    const MAP = MAPS[mapIndex];

    for (let row = 0; row < MAP_H; row++) {
        for (let col = 0; col < MAP_W; col++) {
            const t = MAP[row][col];
            const img = TILE_IMGS[t] || TILE_IMGS[0];
            ctx.drawImage(img, col * STILE, row * STILE, STILE, STILE);
        }
    }
}

function drawPlayers() {
    const me = players[username];
    const myLocation = me ? me.location : 0;

    Object.values(players).forEach(p => {
        if (p.location !== myLocation) return;

        const px = p.x * STILE;
        const py = p.y * STILE;

        off.clearRect(0, 0, STILE, STILE);
        off.globalCompositeOperation = 'source-over';
        off.globalAlpha = 1.0;
        off.drawImage(playerImg, 0, 0, STILE, STILE);
        off.globalCompositeOperation = 'source-atop';
        off.globalAlpha = 0.8;
        off.fillStyle = p.colour;
        off.fillRect(0, 0, STILE, STILE);
        off.globalCompositeOperation = 'source-over';
        off.globalAlpha = 1.0;

        ctx.drawImage(offscreen, px, py);
        ctx.drawImage(playerLinesImg, px, py, STILE, STILE);

        ctx.font = 'bold 12px "Press Start 2P"';
        ctx.textAlign = 'center';
        const nameWidth = ctx.measureText(p.tag).width;
        ctx.fillStyle = 'rgba(0,0,0,0.6)';
        ctx.fillRect(px + STILE/2 - nameWidth/2 - 3, py - STILE*0.4, nameWidth + 6, 13);
        ctx.fillStyle = p.colour;
        ctx.fillText(p.tag, px + STILE/2, py - STILE*0.12);
    });
}

function render() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    const me = players[username];

    if (me && me.activity === 110 && wantsToDoActivity) {
        drawArchery();
    } else {
        drawMap();
        drawPlayers();
    }
}


function updatePlayerList() {
    const list = document.getElementById('player-list');
    list.innerHTML = Object.values(players).map(p =>
        `<div class="player-entry" style="color:${p.colour}">${p.tag}</div>`
    ).join('');
}


function appendChat(msg) {
    const box = document.getElementById('chat-messages');
    const div = document.createElement('div');

    if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
        div.className = msg.type === 'JOIN' ? 'msg-join' : 'msg-leave';
        div.textContent = msg.content;
    } else {
        div.className = 'msg-chat';
        const senderColour = players[msg.sender]?.colour || '#d4af37';
        div.innerHTML = `<span class="sender" style="color:${senderColour}">${msg.sender}:</span> ${msg.content}`;
    }

    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

function sendChat() {
    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    if (!text || !stompClient) return;
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        sender: username,
        content: text,
        type: 'CHAT',
    }));
    input.value = '';
}

function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        stompClient.subscribe('/topic/players', function (payload) {
            const list = JSON.parse(payload.body);
            const oldLocation = players[username]?.location;
            players = {};
            list.forEach(p => players[p.tag] = p);

            if (players[username]?.location !== oldLocation) {
                stopArchery();
            }

            render();
            updatePlayerList();
        });

        stompClient.subscribe('/topic/chat', function (payload) {
            appendChat(JSON.parse(payload.body));
        });

        stompClient.send('/app/player.addPlayer', {}, JSON.stringify({
            tag: username,
            x: 2, y: 2,
        }));

        stompClient.send('/app/chat.join', {}, JSON.stringify({
            sender: username,
            content: '',
            type: 'JOIN',
        }));
    });
}

const KEY_DIR = {
    'ArrowUp': 0, 'w': 0, 'W': 0,
    'ArrowLeft': 1, 'a': 1, 'A': 1,
    'ArrowDown': 2, 's': 2, 'S': 2,
    'ArrowRight': 3, 'd': 3, 'D': 3,
};

const MOVE_DELAY = 150;
const lastMoves = { 0: 0, 1: 0, 2: 0, 3: 0 };

document.addEventListener('keydown', function (e) {
    if (document.activeElement === document.getElementById('chat-input')) return;
    if (document.activeElement === document.getElementById('username-input')) return;

    const me = players[username];
    const inMinigame = me && me.activity === 110 && wantsToDoActivity;

    // Escape always exits
    if (e.key === 'Escape') {
        stopArchery();
        render();
        return;
    }

    if (e.key.toLowerCase() === 'e') {
        if (me && me.mode === 1) {
            wantsToDoActivity = true;
            startArchery(); // defined in archery.js
        }
        return;
    }

    if (inMinigame) {
        if (e.key === ' ' && !ARCHERY.charging) {
            ARCHERY.charging = true;
            e.preventDefault();
        }
    }

    const dir = KEY_DIR[e.key];
    if (dir !== undefined && stompClient) {
        e.preventDefault();
        const now = Date.now();
        if (now - lastMoves[dir] >= MOVE_DELAY) {
            lastMoves[dir] = now;
            stompClient.send('/app/player.movePlayer', {}, dir);
        }
    }
});

document.addEventListener('keyup', function (e) {
    if (e.key === ' ') {
        const me = players[username];
        if (me && me.activity === 110 && wantsToDoActivity && ARCHERY.charging) {
            ARCHERY.charging = false;
            archeryShoot(); // defined in archery.js
        }
    }
});

document.getElementById('chat-input').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') sendChat();
});

document.getElementById('chat-send').addEventListener('click', sendChat);

// ─── Login ───────────────────────────────────────────────────────────────────
function joinGame() {
    const val = document.getElementById('username-input').value.trim();
    if (!val) return;
    username = val;
    document.getElementById('login-overlay').style.display = 'none';
    render();
    connect();
}

document.getElementById('join-btn').addEventListener('click', joinGame);
document.getElementById('username-input').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') joinGame();
});

render();