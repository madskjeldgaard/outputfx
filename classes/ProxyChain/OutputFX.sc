OFX_OutputFX {
	classvar <all;
	var <group, <numChannels, <busIndex, <server, <proxyChain;
	var <checkingBadValues = true, <badSynth, badDefName;

	*initClass {
		all = IdentityDictionary.new;
	}

	*default { ^all[Server.default.name] }


		// only one OutputFX per server ATM.
		// This could be changed if different OutputFX
		// for different groups of output channels are to be used.

	*new { |server, numChannels, slotNames, busIndex|
		var serverName, fx;
		server = server ?? { Server.default };
		case { server.isKindOf(Server) } {
			serverName = server.name
		} { server.isKindOf(Symbol) } {
			serverName = server;
			server = Server.named[serverName];
			if (server.isNil) {
				"*** OutputFX: could not find server for % !\n".postf(serverName.cs);
				^nil
			}
		};

		fx = all[serverName];

		if (fx.notNil) {
			if ( [numChannels, slotNames, busIndex].any(_.notNil) ) {
				"// OutputFX for server % exists, cannot change its settings while running - use\n"
				"OutputFX.clear(%) \n// to make a new one.\n"
				.postf(server.name, server.name.asCompileString);
			};
			^fx
		} {
			^this.make(server, numChannels, slotNames, busIndex ? 0)
		}
	}

	*make { |server, numChannels, slotNames, busIndex|
		^super.new.init(server, numChannels, slotNames, busIndex);
	}

	makeBus {
		^Bus.new(\audio, busIndex, numChannels, server);
	}

	key { ^all.findKeyForValue(this) }
	storeArgs { ^[server.name] }
	printOn { |stream| ^this.storeOn(stream) }

	// interface to proxyChain

	add { |key, wet, func|
		proxyChain.add(key, wet, func);
	}
	remove { |key|
		proxyChain.remove(key);
	}

	set { |...args| proxyChain.set(*args) }

    fadeTime_ { |time| proxyChain.fadeTime_(time) }
    xset { |...args| proxyChain.xset(*args) }

	slotNames { ^proxyChain.slotNames }

	slotNames_ { |argSlotNames| proxyChain.slotNames_(argSlotNames) }

	proxy { ^proxyChain.proxy }

	pxChain { ^proxyChain } // backwards compatibility

	slotsInUse { ^proxyChain.slotsInUse }


	// cmdPeriod {
	// 	group.freeAll; 	// for SharedServers
	// 	// evil just to wait? hmmm.
	// 	server.sync;
	// 	this.wakeUp;
	// 	// defer({ this.wakeUp }, 0.2);
	// }

	// hide Ndef by default
	hide {
		Ndef.all[server.name].envir.removeAt(proxyChain.proxy.key);
	}
	// show it in case that is useful in some circumstances?
	show {
		Ndef.all[server.name].envir.put(proxyChain.proxy.key, proxyChain.proxy);
	}

	init { |inServer, inNumChannels, inSlotNames, inBusIndex|
		var proxy;
		server = inServer ? Server.default;
		numChannels = inNumChannels ? server.options.numOutputBusChannels;
		busIndex = inBusIndex ? 0;

		proxy = Ndef(\ofx_main_output -> server.name);
		proxy.ar(numChannels);
		proxy.bus_(this.makeBus);
		proxyChain = OFX_Chain.from(proxy, inSlotNames ? []);

		this.hide;	// hide by default

		all.put(server.name, this);

		this.makeGroup;
		ServerTree.add({ this.wakeUp });
		// CmdPeriod.add(this);

		badDefName = ("BadOutputFX_" ++ server.name).asSymbol;
		SynthDef(badDefName, {
			var snd = In.ar(busIndex, numChannels);
			var dt = 0.001;
			var isOK = (CheckBadValues.ar(snd) < 0.001);
			var gate = (isOK * DelayN.ar(isOK, dt * 2));
			var outSnd = 	DelayL.ar(snd, dt) * gate;
			ReplaceOut.ar(busIndex, outSnd)
		}).add;

		fork {
			0.2.wait;
			this.checkBad(checkingBadValues);
		};
	}

	makeGroup {
		group = Group.new(server.defaultGroup, \addAfter).isPlaying_(true);
		proxyChain.proxy.parentGroup_(group);
	}

	wakeUp {
		"\nOutputFX for server % waking up.\n\n".postf(server.name);
		this.makeGroup;
		server.sync;
		0.1.wait;
		proxyChain.proxy.wakeUp;
		server.sync;
		0.1.wait;
		this.checkBad;
	}

	clear {
		CmdPeriod.remove(this);
		proxyChain.proxy.clear;
		all.removeAt(proxyChain.proxy.server.name);
	}

	*clear { |name|
		(name ?? { all.keys }).do { |name|
			all.removeAt(name).clear;
		};
	}

	makeName {
		^(this.class.name ++ "_" ++ server.name
			++ "_" ++ proxyChain.proxy.numChannels).asSymbol
	}

    // @TODO
    gui{
      proxyChain.gui
    }
	// gui { |name, numItems, buttonList, parent, bounds, makeSkip = true|
	// 	// the effects are all on by default:
	// 	buttonList = buttonList ?? { proxyChain.slotNames.collect ([_, \slotCtl]) };
	// 	name = name ?? { this.makeName };
	// 	numItems = numItems ? 16;
	// 	^OutputFXGui(this, numItems, parent, bounds, makeSkip, buttonList)
	// 	.name_(name);
	// }

	checkBad { |flag = true|
		checkingBadValues = flag;
		badSynth.free;
		if (checkingBadValues) {
			badSynth = Synth(badDefName, target: group, addAction: \addAfter);
		};
	}

}
