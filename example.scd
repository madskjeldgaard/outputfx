// First, a source sound for testing

// Now create the FXChain for the output
(
s.waitForBoot{

Pbind(\dur, 0.075, \degree, Pseq((-10..10),inf), \pan, Pseg(Pseq([-1.0,1.0],inf), 4, \lin, inf)).play;

OFX_Chain.loadDefaultChains();

// Create a main outputfx with random chain
m = OFX_OutputFX.new(
	server: s,
	numChannels: 2,
	slotNames: [\ofx_chorus, \ofx_timemachine, \ofx_dfm1, \ofx_jpverb ],
	busIndex: 0
);

g = OFX_ChainGui.new(m);//.gui;
}
)

OFX_Chain.sourceDicts.keys.asArray.scramble

LineLayout
