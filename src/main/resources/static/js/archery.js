const ARCHERY = {
    arrows: 5,
    score: 0,
    power: 0,
    charging: false,
    powerDir: 1,
    shots: [],
    phase: 'aiming',
};

let archeryInterval = null;
const arrowImg = new Image();
arrowImg.src = '/images/arrow.png';

function resetArchery() {
    ARCHERY.arrows = 5;
    ARCHERY.score = 0;
    ARCHERY.power = 0;
    ARCHERY.charging = false;
    ARCHERY.powerDir = 1;
    ARCHERY.shots = [];
    ARCHERY.phase = 'aiming';
}

function startArchery() {
    resetArchery();
    archeryInterval = setInterval(() => {
        if (ARCHERY.charging && ARCHERY.phase === 'aiming') {
            ARCHERY.power += 2 * ARCHERY.powerDir;
            if (ARCHERY.power >= 100) { ARCHERY.power = 100; ARCHERY.powerDir = -1; }
            if (ARCHERY.power <= 0)   { ARCHERY.power = 0;   ARCHERY.powerDir = 1;  }
        }
        drawArchery();
    }, 16);
}

function stopArchery() {
    clearInterval(archeryInterval);
    archeryInterval = null;
    wantsToDoActivity = false;
}

function archeryShoot() {
    if (ARCHERY.arrows <= 0 || ARCHERY.phase !== 'aiming') return;

    const centerX = canvas.width / 2;
    const centerY = canvas.height / 3;

    const distFromMiddle = 50 - ARCHERY.power;
    const hitX = centerX;
    const hitY = centerY + distFromMiddle * 1.6;

    const dist = Math.abs(hitY - centerY);
    let points = 0;
    if (dist < 10)      points = 20;
    else if (dist < 25) points = 15;
    else if (dist < 40) points = 8;
    else if (dist < 55) points = 4;
    else if (dist < 70) points = 1;

    ARCHERY.shots.push({ x: hitX, y: hitY, points });
    ARCHERY.score += points;
    ARCHERY.arrows--;
    ARCHERY.power = 0;
    ARCHERY.powerDir = 1;

    if (ARCHERY.arrows <= 0) ARCHERY.phase = 'result';
}

function drawArchery() {
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 3;

    // Background
    ctx.fillStyle = '#518C51';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Target stand
    ctx.fillStyle = '#8B4513';
    ctx.fillRect(centerX - 4, centerY + 40, 8, 60);

    // Target rings
    ctx.fillStyle = 'black';
    ctx.fillRect(centerX - 70, centerY - 70, 140, 140);
    ctx.fillStyle = 'white';
    ctx.fillRect(centerX - 55, centerY - 55, 110, 110);
    ctx.fillStyle = 'blue';
    ctx.fillRect(centerX - 40, centerY - 40, 80, 80);
    ctx.fillStyle = '#cc0000';
    ctx.fillRect(centerX - 25, centerY - 25, 50, 50);
    ctx.fillStyle = '#f5c518';
    ctx.fillRect(centerX - 10, centerY - 10, 20, 20);

    // Ring dividers
    ctx.strokeStyle = 'rgba(0,0,0,0.3)';
    ctx.lineWidth = 1;
    ctx.strokeRect(centerX - 40, centerY - 40, 80, 80);
    ctx.strokeRect(centerX - 25, centerY - 25, 50, 50);
    ctx.strokeRect(centerX - 10, centerY - 10, 20, 20);

    ARCHERY.shots.forEach(s => {
        ctx.drawImage(arrowImg, s.x - 16, s.y);
        });

    //Power bar
    const barX = 20;
    const barY = canvas.height - 50;
    const barW = 200;
    const barH = 18;

    ctx.font = '7px "Press Start 2P"';
    ctx.fillStyle = '#ccc';
    ctx.textAlign = 'left';
    ctx.fillText('POWER', barX, barY - 6);

    // Bar background
    ctx.fillStyle = '#222';
    ctx.fillRect(barX, barY, barW, barH);

    // Bar fill green → yellow → red
    const r = Math.floor(ARCHERY.power * 2.55);
    const g = Math.floor((100 - ARCHERY.power) * 2.55);
    ctx.fillStyle = `rgb(${r},${g},0)`;
    ctx.fillRect(barX, barY, ARCHERY.power * (barW / 100), barH);

    // Bar border
    ctx.strokeStyle = '#d4af37';
    ctx.lineWidth = 1;
    ctx.strokeRect(barX, barY, barW, barH);

    ctx.textAlign = 'center';
    ctx.font = '7px "Press Start 2P"';
    ctx.fillStyle = 'white';
    ctx.fillText(`ARROWS: ${ARCHERY.arrows}   SCORE: ${ARCHERY.score} / 50`, canvas.width / 2, 18);
    ctx.fillStyle = '#aaa';
    ctx.fillText('HOLD SPACE TO CHARGE  •  RELEASE TO SHOOT  •  ESC TO EXIT', canvas.width / 2, 36);

    // Result overlay
    if (ARCHERY.phase === 'result') {
        ctx.fillStyle = 'rgba(0,0,0,0.65)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        ctx.textAlign = 'center';
        ctx.font = '14px "Press Start 2P"';
        ctx.fillStyle = '#d4af37';
        ctx.fillText('ROUND OVER', canvas.width / 2, canvas.height / 2 - 30);

        ctx.font = '9px "Press Start 2P"';
        ctx.fillStyle = 'white';
        ctx.fillText(`FINAL SCORE: ${ARCHERY.score}`, canvas.width / 2, canvas.height / 2 + 4);

        ctx.fillStyle = '#888';
        ctx.fillText('PRESS ESC TO EXIT', canvas.width / 2, canvas.height / 2 + 50);
    }
}