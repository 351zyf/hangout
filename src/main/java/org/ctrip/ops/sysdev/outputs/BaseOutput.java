package org.ctrip.ops.sysdev.outputs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;
import org.ctrip.ops.sysdev.filters.BaseFilter;
import org.ctrip.ops.sysdev.render.FreeMarkerRender;
import org.ctrip.ops.sysdev.render.JinjavaRender;
import org.ctrip.ops.sysdev.render.TemplateRender;

public class BaseOutput implements Runnable {
	private static final Logger logger = Logger.getLogger(BaseOutput.class
			.getName());

	protected Map config;
	protected ArrayBlockingQueue inputQueue;
	protected List<TemplateRender> IF;

	public BaseOutput(Map config, ArrayBlockingQueue inputQueue) {
		this.config = config;

		if (this.config.containsKey("if")) {
			IF = new ArrayList<TemplateRender>();
			for (String c : (List<String>) this.config.get("if")) {
				try {
					IF.add(new FreeMarkerRender(c, c));
				} catch (IOException e) {
					logger.fatal(e.getMessage());
					System.exit(1);
				}
			}
		} else {
			IF = null;
		}

		this.inputQueue = inputQueue;

		this.prepare();
	}

	protected void prepare() {
	};

	@Override
	public void run() {
		try {
			while (true) {
				Map event = (Map) this.inputQueue.take();
				if (event != null) {
					boolean succuess = true;
					if (this.IF != null) {
						for (TemplateRender render : this.IF) {
							if (!render.render(event).equals("true")) {
								succuess = false;
								break;
							}
						}
					}
					if (succuess == true) {
						this.emit(event);
					}
				}
			}
		} catch (InterruptedException e) {
			logger.warn("put event to outMQ failed");
			logger.trace(e.getMessage());
		}
	};

	public void emit(Map event) {
	};
}