package com.github.czyzby.lml.parser.impl.macro;

import com.github.czyzby.lml.parser.LmlParser;
import com.github.czyzby.lml.parser.impl.dto.LmlMacroData;
import com.github.czyzby.lml.parser.impl.dto.LmlParent;
import com.github.czyzby.lml.parser.impl.macro.parent.AbstractImportLmlMacroParent;
import com.github.czyzby.lml.parser.impl.macro.parent.ClasspathImportLmlMacroParent;

public class ClasspathImportLmlMacroParser extends AbstractImportLmlMacroParser {
	@Override
	protected AbstractImportLmlMacroParent getImportMacro(final LmlMacroData lmlMacroData,
			final LmlParent<?> parent, final LmlParser parser) {
		return new ClasspathImportLmlMacroParent(lmlMacroData, parent, parser);
	}
}