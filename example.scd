// First, a source sound for testing

// Now create the FXChain for the output
(
s.doWhenBooted{
Pbind(\dur, 0.075, \degree, Pseq((-10..10),inf), \pan, Pseg(Pseq([-1.0,1.0],inf), 4, \lin, inf)).play;

OFX_Chain.add3(
    srcName: \pitchshift, 
	source: \filter -> { |in| 
		PitchShift.ar(in: in,  windowSize: 0.2,  pitchRatio: \pitch.kr(0.5, spec: [0.001,4.0]),  pitchDispersion: 0.5,  timeDispersion: 0.0,  mul: 1.0,  add: 0.0);
	},
	level: 0.1,
	// specs: (
	// 	time: [0.0001,100.0,\exp]
	// )
);

OFX_Chain.add3(
    srcName: \jpverb, 
	source: \filter -> { |in, time=10, damp=0.5, size=3| 
		JPverb.ar(
			in: in,  
			t60: time,  
			damp: damp,  size: size,  earlyDiff: 0.707,  modDepth: \modDepth.kr(0.25),  
			modFreq: \modFreq.kr(0.1, spec: [0.0,10.0]),  
			low: 1.0,  
			mid: 1.0,  
			high: 1.0,  
			lowcut: 500.0,  
			highcut: 2000.0
		);
	},
	level: 0.1,
	specs: (
		time: [0.0001,100.0,\exp],
		damp: [0.0,1.0,\lin],
		size: [0.0001,5.0,\exp]

	)
);

// Now add the fx chain to the output
m = OFX_OutputFX.new(
	server: s,
	numChannels: 2,
	slotNames: [\pitchshift, \jpverb],
	busIndex: 0
);

g = OFX_ChainGui.new(m.proxyChain);

}
)

g.didNewSlotsBecomeActive

OFX_Chain.atSrcDict(\jpverb).keys

m.proxyChain.keysValuesAt(\jpverb)
g.slotNames

m.proxyChain.slotNames = [ \jpverb]

// You won't hear anything yet because the fx haven't been activated, this is done using .add:
m.add(\jpverb, 0.3);
m.add(\pitchshift, 0.25);

// Now change pitchshift
m.set(\pitch, 4);

// Change time param of verb
m.proxyChain.set(\time, rrand(0.1,100));
g.guiData[\jpverb]

m.proxyChain.keysValuesAt(\jpverb)
g.guiData

// Change size param of verb
m.set(\size, rrand(0.001,5));

OFX_Chain.atSrcDict(\jpverb)[\source].value.defaultArgs

m.proxyChain.proxy.gui
