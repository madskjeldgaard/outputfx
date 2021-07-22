// First, a source sound for testing
Pbind(\dur, 0.075, \degree, Pseq((-10..10),inf), \pan, Pseg(Pseq([-1.0,1.0],inf), 4, \lin, inf)).play;

// Now create the FXChain for the output
(
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
	source: \filter -> { |in| 
		JPverb.ar(
			in: in,  
			t60: \time.kr(10, spec: [0.0001,100.0,\exp]),  
			damp: \damp.kr(0.25),  size: \size.kr(1),  earlyDiff: 0.707,  modDepth: \modDepth.kr(0.25),  
			modFreq: \modFreq.kr(0.1, spec: [0.0,10.0]),  
			low: 1.0,  
			mid: 1.0,  
			high: 1.0,  
			lowcut: 500.0,  
			highcut: 2000.0
		);
	},
	level: 0.1,
	// specs: (
	// 	time: [0.0001,100.0,\exp]
	// )
);

)
(
// Now add the fx chain to the output
m = OFX_OutputFX.new(
	server: s,
	numChannels: 2,
	slotNames: [\pitchshift, \jpverb],
	busIndex: 0
);

)

// You won't hear anything yet because the fx haven't been activated, this is done using .add:
m.add(\jpverb, 0.3);
m.add(\pitchshift, 0.25);

// Now change pitchshift
m.set(\pitch, 4);

// Change time param of verb
m.set(\time, rrand(1,50));

// Change size param of verb
m.set(\size, rrand(0.001,5));
