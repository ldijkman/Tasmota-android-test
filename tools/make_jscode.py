#!/usr/bin/env python3
"""Colour site/shelly-thermostat-example.js and put it in the code box (pre.jscode) of site/index.html."""
import re, html, pathlib
root = pathlib.Path(__file__).resolve().parent.parent
src = (root / 'site/shelly-thermostat-example.js').read_text()
KW = {'let', 'function', 'if', 'else', 'return', 'true', 'false', 'null', 'for', 'typeof', 'undefined', 'in', 'new', 'var', 'while'}
OBJ = {'Shelly', 'Timer', 'HTTPServer', 'JSON', 'Math', 'Number', 'String', 'console'}
tok = re.compile(r'(//[^\n]*)|("(?:[^"\\\n]|\\.)*"|\'(?:[^\'\\\n]|\\.)*\')|(\b\d+(?:\.\d+)?\b)|([A-Za-z_$][\w$]*)|([\s\S])')
out = []
for m in tok.finditer(src):
    c, s, n, w, o = m.groups()
    e = lambda t: html.escape(t, quote=True)
    if c: out.append(f'<span class="jc">{e(c)}</span>')
    elif s: out.append(f'<span class="js">{e(s)}</span>')
    elif n: out.append(f'<span class="jn">{n}</span>')
    elif w: out.append(f'<span class="jk">{w}</span>' if w in KW else f'<span class="jf">{w}</span>' if w in OBJ else w)
    else: out.append(e(o))
code = ''.join(out).rstrip('\n')
p = root / 'site/index.html'; s = p.read_text()
a = s.index('<pre class="jscode" lang="en" translate="no"><code>') + len('<pre class="jscode" lang="en" translate="no"><code>')
b = s.index('</code></pre>', a)
s = s[:a] + code + s[b:]
p.write_text(s)
print('code box updated,', src.count('\n') + 1, 'lines')
