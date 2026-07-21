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
    120: new Image(),
};
TILE_IMGS[0].src   = '/images/grass.png';
TILE_IMGS[1].src   = '/images/tree.png';
TILE_IMGS[2].src   = '/images/water.png';
TILE_IMGS[3].src   = '/images/breeGrass.png';
TILE_IMGS[4].src   = '/images/breeTree.png';
TILE_IMGS[10].src  = '/images/portal.png';
TILE_IMGS[20].src  = '/images/portal.png';
TILE_IMGS[30].src  = '/images/portal.png';

const MAPS = [
    [
        [1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
        [1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1],
        [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],
        [1,0,0,1,1,0,0,0,0,1,1,0,0,0,1,0,0,0,0,1],
        [1,0,0,1,1,0,0,0,0,1,1,120,120,0,1,0,0,0,0,1],
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

function drawMap() {
    const me = players[username];
    const mapIndex = me ? me.location : 0;
    const MAP = MAPS[mapIndex];

    for (let row = 0; row < MAP_H; row++) {
        for (let col = 0; col < MAP_W; col++) {
            const t = MAP[row][col];
            if (t === 120) {
                ctx.fillStyle = '#ffffff';
                ctx.fillRect(col * STILE, row * STILE, STILE, STILE);
            } else {
                const img = TILE_IMGS[t] || TILE_IMGS[0];
                ctx.drawImage(img, col * STILE, row * STILE, STILE, STILE);
            }
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

    drawMap();
    drawPlayers();
    if (me && me.encounterBeast) drawWildEncounter(me);
    if (me && me.opponent) drawArenaBattle(me);
    if (me && me.activity === 120 && !me.opponent && !me.encounterBeast) drawArenaWaiting(me);
    if (me && me.choosingBeast) drawBeastChoice(me);
}


function updatePlayerList() {
    const list = document.getElementById('player-list');
    list.replaceChildren(...Object.values(players).map(p => {
        const entry = document.createElement('div');
        entry.className = 'player-entry';
        entry.style.color = p.colour;
        entry.textContent = p.tag;
        return entry;
    }));
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
        const sender = document.createElement('span');
        sender.className = 'sender';
        sender.style.color = senderColour;
        sender.textContent = `${msg.sender}:`;
        div.append(sender, document.createTextNode(` ${msg.content}`));
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
            players = {};
            list.forEach(p => players[p.tag] = p);

            render();
            updatePlayerList();
            updateBeastList();
        });

        stompClient.subscribe('/topic/chat', function (payload) {
            appendChat(JSON.parse(payload.body));
        });

        stompClient.send('/app/player.addPlayer', {}, JSON.stringify({
            tag: username,
            beasts: [{ type: document.getElementById('beast-input').value }],
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
    if (!me) {
        return;
    }

    if (me.encounterBeast && e.key.toLowerCase() === 's') {
        stompClient.send('/app/player.skipEncounter', {});
        return;
    }

    const attack = Number.parseInt(e.key, 10) - 1;
    if (me.choosingBeast && attack >= 0 && attack < me.beasts.length && stompClient) {
        stompClient.send('/app/player.chooseBeast', {}, attack);
        return;
    }
    if (attack >= 0 && attack <= 2 && stompClient) {
        if (me.encounterBeast) stompClient.send('/app/player.wildAttack', {}, attack);
        else if (me.opponent && me.turn) stompClient.send('/app/player.arenaAttack', {}, attack);
        return;
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

document.getElementById('chat-input').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') sendChat();
});

document.getElementById('chat-send').addEventListener('click', sendChat);
document.getElementById('beasts-toggle').addEventListener('click', function () {
    const panel = document.getElementById('beasts-panel');
    panel.hidden = !panel.hidden;
});

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

const BEAST_COLOURS = {
    FIRE: '#e74c3c',
    WATER: '#3498db',
    EARTH: '#9b59b6',
};

function drawBlob(x, y, type) {
    ctx.fillStyle = BEAST_COLOURS[type] || '#ffffff';
    ctx.beginPath();
    ctx.arc(x, y, STILE / 2 - 2, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#222';
    ctx.fillRect(x - 10, y - 4, 5, 5);
    ctx.fillRect(x + 5, y - 4, 5, 5);
}

function drawPanel(title, lines) {
    ctx.fillStyle = 'rgba(0,0,0,0.82)';
    ctx.fillRect(32, canvas.height - 160, canvas.width - 64, 128);
    ctx.strokeStyle = '#d4af37';
    ctx.strokeRect(32, canvas.height - 160, canvas.width - 64, 128);
    ctx.fillStyle = '#d4af37';
    ctx.font = '12px "Press Start 2P"';
    ctx.textAlign = 'left';
    ctx.fillText(title, 52, canvas.height - 132);
    ctx.fillStyle = '#ffffff';
    ctx.font = '8px "Press Start 2P"';
    lines.forEach((line, index) => ctx.fillText(line, 52, canvas.height - 100 + index * 22));
}

function attacksFor(type) {
    if (type === 'FIRE') return '1: EMBER   2: FLAME BURST   3: INFERNO';
    if (type === 'WATER') return '1: SPLASH   2: WAVE   3: TSUNAMI';
    return '1: PEBBLE   2: QUAKE   3: LANDSLIDE';
}

function activeBeast(player) {
    return player.beasts[player.activeBeastIndex];
}

function drawHealthBar(x, y, beast) {
    const width = 150;
    ctx.fillStyle = '#222';
    ctx.fillRect(x, y, width, 12);
    ctx.fillStyle = beast.hp > 50 ? '#2ecc71' : beast.hp > 20 ? '#f39c12' : '#e74c3c';
    ctx.fillRect(x, y, width * beast.hp / 100, 12);
    ctx.strokeStyle = '#ffffff';
    ctx.strokeRect(x, y, width, 12);
    ctx.fillStyle = '#ffffff';
    ctx.font = '7px "Press Start 2P"';
    ctx.textAlign = 'left';
    ctx.fillText(`${beast.type} ${beast.hp} / 100`, x, y - 6);
}

function drawWildBeastFrame(x, y, beast) {
    const size = STILE;
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.strokeRect(x - size / 2, y - size / 2, size, size);
    ctx.fillStyle = '#111';
    ctx.fillRect(x - size / 2, y - size / 2 - 18, size, 15);
    ctx.fillStyle = '#ffffff';
    ctx.font = '7px "Press Start 2P"';
    ctx.textAlign = 'center';
    ctx.fillText(beast.type, x, y - size / 2 - 7);
}

function drawWildEncounter(me) {
    const beast = activeBeast(me);
    drawBlob(canvas.width / 2, canvas.height / 3, me.encounterBeast.type);
    drawWildBeastFrame(canvas.width / 2, canvas.height / 3, me.encounterBeast);
    drawHealthBar(60, 70, beast);
    drawHealthBar(canvas.width - 210, 70, me.encounterBeast);
    drawPanel(`WILD ${me.encounterBeast.type} BEAST`, [
        `YOUR ${beast.type} BEAST IS FIGHTING`,
        attacksFor(beast.type),
        'S: SKIP ENCOUNTER',
    ]);
}

function drawArenaBattle(me) {
    const opponent = players[me.opponent];
    if (!opponent) return;
    const beast = activeBeast(me);
    const opponentBeast = activeBeast(opponent);
    if (!beast || !opponentBeast) return;
    drawBlob(canvas.width / 3, canvas.height / 3, beast.type);
    drawBlob(canvas.width * 2 / 3, canvas.height / 3, opponentBeast.type);
    drawHealthBar(60, 70, beast);
    drawHealthBar(canvas.width - 210, 70, opponentBeast);
    drawPanel(`ARENA: ${beast.type} VS ${opponentBeast.type}`, [
        `${me.tag} VS ${opponent.tag}`,
        me.turn ? `YOUR TURN: ${attacksFor(beast.type)}` : 'WAITING FOR OPPONENT',
        me.battleStatus || '',
    ]);
}

function drawArenaWaiting(me) {
    drawPanel('ARENA', [me.battleStatus || 'WAITING FOR AN OPPONENT', 'A SECOND PLAYER MUST ENTER THE WHITE ARENA']);
}

function drawBeastChoice(me) {
    const options = me.beasts.map((beast, index) => `${index + 1}: ${beast.type} ${beast.hp} / 100`).join('     ');
    drawPanel('CHOOSE YOUR NEXT BEAST', [options, 'PRESS THE NUMBER FOR A BEAST WITH HP LEFT']);
}

function updateBeastList() {
    const list = document.getElementById('beast-list');
    const me = players[username];
    if (!me) {
        list.replaceChildren();
        return;
    }
    list.replaceChildren(...me.beasts.map((beast, index) => {
        const entry = document.createElement('div');
        entry.className = 'beast-entry';
        entry.textContent = `${index + 1}. ${beast.type} ${beast.hp} / 100`;
        const bar = document.createElement('div');
        bar.className = 'beast-hp';
        const fill = document.createElement('div');
        fill.className = 'beast-hp-fill';
        fill.style.width = `${beast.hp}%`;
        fill.style.background = BEAST_COLOURS[beast.type];
        bar.appendChild(fill);
        entry.appendChild(bar);
        return entry;
    }));
}
