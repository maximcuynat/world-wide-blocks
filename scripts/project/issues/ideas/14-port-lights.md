Title: Port lights in the finishing lot
Labels: type: idea

Lights on each port of the models, in lot F (finishing): steady green when a cable is connected, blinking green when frames go through, driven by a frame counter per port in the engine, orange when the negotiated speed is lower than the port's or something is wrong with the link, fast blinking orange on a port involved in a detected loop. The counter could also feed the monitoring server idea and the future frame analyzer.

Base laid by the design work: each LED has a fixed position, to the texel, in the device sheet (`docs/design-guide.md`, section 5; issue #56), so that its off and blinking variants are simple to produce; the port layout (#61) ties each port to its position, and the client already draws per-port details from the cables it knows (plugs, #63).
