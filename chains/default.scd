/*

These are the default OutputFX chain effects. 

ADDING NEW FX
- Please prefix them with \ofx_ like you see below
- Write good specs for everything

SUGGESTED SERVER SETTINGS:

s.options.numWireBufs_(2048);
s.options.memSize_(2.pow(19));
s.options.numBuffers_(10000);

*/
(

"Loading default OFX chains".postln;

// Nice spec for lfo frequencies
ControlSpec.add(\lfoFreq, [0.000001, 20.0, \exp]);

// dfm1 filter
OFX_Chain.add3(
	srcName: \ofx_dfm1,
	source: \filter -> {|in, inputgain=1.0, freq=500, res=0.1, type=0, noise=0.0| 
		DFM1.ar(in: in,  freq: freq,  res: res,  inputgain: inputgain,  type: type,  noiselevel: noise);
	},
	level: 1.0,
	specs: (
		inputgain: [0.0, 1.0], freq: Spec.specs[\freq], res: [0.0, 1.0], type: [0, 1], noise: [0.0, 1.0],	
	)
);

// dc blocker
OFX_Chain.add3(
	srcName: \ofx_dc,
	source: \filter -> {|in, coef=0.995|
		LeakDC.ar(in: in,  coef: coef);
	},
	level: 1.0,
	specs: (
		coef: [0.0,1.0]
	)
);

// jpverb
OFX_Chain.add3(
    srcName: \ofx_jpverb, 
	source: \filter -> { |in, time=10, damp=0.5, size=3, earlyDiff=0.707,  modDepth=0.1,  modFreq=2, low=1,  mid=1,  high=1,  lowcut=500, highcut=2000| 

		JPverb.ar(
			in: in,  
			t60: time,  
			damp: damp, 
			size: size,  
			earlyDiff: earlyDiff,  
			modDepth: modDepth,  
			modFreq: modFreq,
			low: low,  
			mid: mid,  
			high: high,  
			lowcut: lowcut,
			highcut: highcut
		);

	},
	level: 0.5,
	specs: (
		time: [0.0001, 100.0, \exp],
		damp: [0.0, 1.0, \lin],
		size: [0.0001, 5.0, \exp],
		earlyDiff: [0.0, 1.0, \lin],  
		modDepth: [0.0, 1.0, \lin],  
		modFreq: [0.0001, 10.0, \exp], 
		low: [0.0, 1.0],  
		mid: [0.0, 1.0],  
		high: [0.0, 1.0],  
		lowcut: Spec.specs[\freq], 
		highcut: Spec.specs[\freq]
	)
);

// pitchshift
OFX_Chain.add3(
	srcName: \ofx_pitchshift,
	source: \filter ->{|in=0.0, windowSize=0.2, pitchRatio=1.0, pitchDispersion=0.0, timeDispersion=0.0|	
		PitchShift.ar(in: in,  windowSize: windowSize,  pitchRatio: pitchRatio,  pitchDispersion: pitchDispersion,  timeDispersion: timeDispersion);
	},
	level: 0.5,
	specs:(
		windowSize: [0.001, 1.0], pitchRatio: [0.0, 4.0], pitchDispersion: [0.0, 1.0], timeDispersion: [0.0, 1.0],
	)
);

// freqshift
OFX_Chain.add3(
	srcName: \ofx_freqshift,
	source: \filter ->{	|in, freq=0.0, phase=0|		
		FreqShift.ar(in: in,  freq: freq,  phase: phase);
	},
	level: 0.5,
	specs:(
		freq: Spec.specs[\widefreq], phase: Spec.specs[\phase],	
	)
);

// octer
// From SonicPi via SuperDirt
OFX_Chain.add3(
	srcName: \ofx_octer,
	source: \filter -> {|in, octer=0.5, octersub=0.0, octersubsub=0.0|
			var sig, oct1, oct2, oct3, sub;
			sig = in;
			oct1 = 2.0 * LeakDC.ar( abs(sig) );
			sub = LPF.ar(sig, 440);
			oct2 = ToggleFF.ar(sub);
			oct3 = ToggleFF.ar(oct2);
			sig = SelectX.ar(octer, [sig, octer * oct1, DC.ar(0)]);
			sig = sig + (octersub * oct2 * sub) + (octersubsub * oct3 * sub);
			sig
		},
	level: 0.5,
	specs:(
		octer: [0.0, 2.0], octersub: [0.0, 1.0], octersubsub: [0.0, 1.0],	
	)
);

// ringmod1
OFX_Chain.add3(
	srcName: \ofx_ringmod1,
	level: 0.5,
	source: \filter -> { |in, ringf = 0.5|
			var mod = SinOsc.ar(ringf);
			ring1(in, mod);
		}, 
	specs: (ringf: Spec.specs[\freq])
);

// A time machine effect (inspired by Make Noise' mimeophone)
OFX_Chain.add3(
	srcName: \ofx_timemachine,
	source: \filter -> {|in, offset=0, reclvl=0.5, prelvl=0.5, timerate=0.5, fb=0.25, record=1, jumpToStart=1|
		var numchans = in.size;
		var local = LocalBuf(44100 * 16, numChannels: numchans).clear;
		var sig;
		var feedback = LocalIn.ar(fb * in, numChannels: numchans).tanh;

		RecordBuf.ar(in + feedback, local, offset, reclvl, prelvl,  run: record,  loop: 1.0,  trigger: jumpToStart,  doneAction: 0);

		sig = PlayBuf.ar(numchans, local, timerate, 1.0, 0.0, 1.0, doneAction: 0);
		sig = LeakDC.ar(sig);

		LocalOut.ar(LeakDC.ar(sig));

		sig
	},
	level: 0.5,
	specs: (
		offset: [0.0, 1.0], reclvl: [0.0, 1.0], prelvl: [0.0, 1.0], timerate: [-10.0, 10.0], fb: [0.0, 1.0], record: [0.0, 1.0], jumpToStart: [0.0, 1.0],
	)
);

// Phaser taken from SuperDirt and modified
// https://github.com/musikinformatik/SuperDirt/blob/develop/synths/core-synths.scd
OFX_Chain.add3(
	srcName: \ofx_phaser,
	source: \filter -> { |in, phaserrate = 1.0, phaserdepth = 0.5|
		var numchans = in.size;

		Array.fill(numchans, {|cNum|
			var phase =  LFPar.kr(phaserrate * cNum.linlin(0, numchans, 0.95,1.05)).range(0.0088, 0.01);
			phaserdepth = phaserdepth.clip(0, 1);

			AllpassC.ar(in[cNum], 4, phase, 0, phaserdepth);
		})
	},
	level: 0.5,
	specs: ( phaserrate: Spec.specs[\lfoFreq], phaserdepth: [0.0, 1.0] )

);

// flanger
// Taken from Thor Magnussons book Scoring Sound: https://leanpub.com/ScoringSound/read#leanpub-auto-flanger 
OFX_Chain.add3(
	srcName: \ofx_flanger,
	source: \filter -> { |in, flangdelay=0.1, flangdepth=0.5, flangrate=0.06, flangfb=0.1|
		var input, maxdelay, maxrate, dsig, mixed, local;
		var numchans = in.size;

		maxdelay = 1.0;
		input = in;
		local = LocalIn.ar(numchans);

		dsig = Array.fill(numchans, {|cNum|
			AllpassC.ar( 
				input[cNum] + (local[cNum] * flangfb),
				maxdelay,
				LFTri.kr( 
					flangrate,
					0,
					flangdepth * maxdelay,
					flangdelay * maxdelay
				),
				0
			);
		});

		LocalOut.ar(dsig);
		dsig;
	},
	level: 0.5,
	specs: ( 
		flangdelay: [0.0, 1.0], flangdepth: [0.0, 1.0], flangrate: Spec.specs[\lfoFreq], flangfb: [0.0, 1.0]
	)
);

OFX_Chain.add3(
	srcName: \ofx_chorus,
	source: \filter -> {|in, chpredelay=0.08, chrate=0.05, chdepth=0.1, chphasediff=0.5|
		var sig, numDelays = 4;
		var numchans = in.size;
		var maxdelay = 0.5;

		sig = Array.fill(numchans, {|cNum|
			var input = in[cNum] * numDelays.reciprocal;
			var modulators = Array.fill(numDelays, { arg i; 
				LFPar.kr(chrate * {rrand(0.94, 1.06)}, chphasediff * i, chdepth, chpredelay)
			});
			DelayC.ar(input, maxdelay, modulators).sum;  
		});

		sig
	},
	level: 0.5,
	specs: (
		chpredelay: [0.0, 1.0], chrate: Spec.specs[\lfoFreq], chdepth: [0.0, 1.0], chphasediff: [0.0, 1.0],
	)
);

// waveloss
OFX_Chain.add3(
	srcName: \ofx_loss,
	source: \filter -> {|in, drop=30, mode=2|  
		WaveLoss.ar(in: in,  drop: drop,  outof: 100,  mode: mode)
	},
	level: 1.0,
	specs: (
		drop: [0,100], mode: [1, 2]
	)
);

// limiter
OFX_Chain.add3(
	srcName: \ofx_limiter,
	source: \filter -> {|in, level=0.99|  
		Limiter.ar(in, level)
	},
	level: 1.0,
	specs: (
		level: [0.0, 1.0]
	)
);


)
