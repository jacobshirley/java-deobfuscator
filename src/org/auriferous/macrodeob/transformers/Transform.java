package org.auriferous.macrodeob.transformers;

import org.auriferous.macrodeob.transformers.base.TransformClassNode;


public interface Transform {	
	public boolean accept(TransformClassNode tcn);
}
