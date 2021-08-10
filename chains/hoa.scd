/*
// Dependencies
Quarks.install("atk-sc3");

*/
(
"Loading default OFX hoa chains".postln;

(1..7).do{|order|

	OFX_Chain.add3(
		"hoaRotateO%".format(order).asSymbol, 
		\filter -> {
			|in, rotation=0|
			var sig = in;

			HoaRotate.ar(sig, rotation, order)
		},
		1,
		(
			\rotation: [(-pi), pi, \lin]
		)
	);

	OFX_Chain.add3(
		"hoaTiltO%".format(order).asSymbol, 
		\filter -> {
			|in, tilt=0|
			var sig = in;

			HoaTilt.ar(sig, tilt, order)
		},
		1,
		(
			\tilt: [(-pi), pi, \lin]
		)
	);

	OFX_Chain.add3(
		"hoaTumbleO%".format(order).asSymbol, 
		\filter -> {
			|in, tumble=0|
			var sig = in;

			HoaTumble.ar(sig, tumble, order)
		},
		1,
		(
			\tumble: [(-pi), pi, \lin]
		)
	);


	OFX_Chain.add3(
		"hoaRollO%".format(order).asSymbol, 
		\filter -> {
			|in, roll=0|
			var sig = in;

			HoaRoll.ar(sig, roll, order)
		},
		1,
		(
			\roll: [(-pi), pi, \lin]
		)
	);

};

)
