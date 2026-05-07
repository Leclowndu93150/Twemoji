const STIFFNESS = 0.18;
const DAMPING = 0.72;
const BOUNCE_STIFFNESS = 0.35;
const BOUNCE_DAMPING = 0.6;

export class SpringDrag {
    constructor(element, { onDrop, onCancel } = {}) {
        this.element = element;
        this.onDrop = onDrop;
        this.onCancel = onCancel;
        this.clone = null;
        this.px = 0; this.py = 0;
        this.vx = 0; this.vy = 0;
        this.mx = 0; this.my = 0;
        this.raf = null;
        this.active = false;
        this._onMouseMove = this._onMouseMove.bind(this);
        this._onMouseUp = this._onMouseUp.bind(this);
    }

    start(mouseX, mouseY) {
        const rect = this.element.getBoundingClientRect();
        this.px = rect.left + rect.width / 2;
        this.py = rect.top + rect.height / 2;
        this.vx = 0; this.vy = 0;
        this.mx = mouseX; this.my = mouseY;
        this.active = true;

        this.clone = this.element.cloneNode(true);
        this.clone.style.cssText = `
            position: fixed;
            pointer-events: none;
            z-index: 9999;
            width: ${rect.width}px;
            height: ${rect.height}px;
            transform-origin: center center;
            will-change: transform;
            transition: none;
            border-radius: 6px;
            overflow: hidden;
            box-shadow: 0 8px 24px #0006;
        `;
        document.body.appendChild(this.clone);
        this.element.style.opacity = "0.3";

        document.addEventListener("mousemove", this._onMouseMove);
        document.addEventListener("mouseup", this._onMouseUp);
        this._tick();
    }

    _onMouseMove(e) {
        this.mx = e.clientX;
        this.my = e.clientY;
    }

    _onMouseUp(e) {
        this.active = false;
        document.removeEventListener("mousemove", this._onMouseMove);
        document.removeEventListener("mouseup", this._onMouseUp);
        cancelAnimationFrame(this.raf);

        this.clone.style.display = "none";
        const target = document.elementFromPoint(e.clientX, e.clientY)?.closest("[data-drop-target]") ?? null;
        this.clone.style.display = "";

        this._animateDrop(target);
    }

    _tick() {
        if (!this.active) return;
        this.vx += (this.mx - this.px) * STIFFNESS;
        this.vy += (this.my - this.py) * STIFFNESS;
        this.vx *= DAMPING;
        this.vy *= DAMPING;
        this.px += this.vx;
        this.py += this.vy;

        const dx = this.mx - this.px;
        const dy = this.my - this.py;
        const dist = Math.sqrt(dx * dx + dy * dy);
        const angle = Math.atan2(dy, dx) * (180 / Math.PI);
        const stretch = Math.min(dist / 50, 0.2);

        const hw = parseFloat(this.clone.style.width) / 2;
        const hh = parseFloat(this.clone.style.height) / 2;
        this.clone.style.left = `${this.px - hw}px`;
        this.clone.style.top = `${this.py - hh}px`;
        this.clone.style.transform = `rotate(${angle * 0.06}deg) scaleX(${1 + stretch}) scaleY(${1 - stretch * 0.4})`;

        this.raf = requestAnimationFrame(() => this._tick());
    }

    _animateDrop(target) {
        let destX, destY;
        if (target) {
            const r = target.getBoundingClientRect();
            destX = r.left + r.width / 2;
            destY = r.top + r.height / 2;
        } else {
            const r = this.element.getBoundingClientRect();
            destX = r.left + r.width / 2;
            destY = r.top + r.height / 2;
        }

        let px = this.px, py = this.py;
        let vx = this.vx * 0.3, vy = this.vy * 0.3;
        const hw = parseFloat(this.clone.style.width) / 2;
        const hh = parseFloat(this.clone.style.height) / 2;

        const bounce = () => {
            vx += (destX - px) * BOUNCE_STIFFNESS;
            vy += (destY - py) * BOUNCE_STIFFNESS;
            vx *= BOUNCE_DAMPING;
            vy *= BOUNCE_DAMPING;
            px += vx;
            py += vy;

            this.clone.style.left = `${px - hw}px`;
            this.clone.style.top = `${py - hh}px`;
            this.clone.style.transform = "scale(1) rotate(0deg)";

            const dist = Math.sqrt((destX - px) ** 2 + (destY - py) ** 2);
            const speed = Math.sqrt(vx * vx + vy * vy);

            if (dist < 1.5 && speed < 0.5) {
                this.clone.remove();
                this.element.style.opacity = "";
                if (target) this.onDrop?.(target);
                else this.onCancel?.();
                return;
            }
            requestAnimationFrame(bounce);
        };
        requestAnimationFrame(bounce);
    }
}
