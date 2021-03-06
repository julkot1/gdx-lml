package com.github.czyzby.lml.parser.impl.attribute.table.cell;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.github.czyzby.lml.parser.LmlParser;
import com.github.czyzby.lml.parser.tag.LmlTag;

/** See {@link Cell#fill(boolean, boolean)}. Mapped to "fill".
 *
 * @author MJ */
public class CellFillLmlAttribute extends AbstractCellLmlAttribute {
    @Override
    public void process(final LmlParser parser, final LmlTag tag, final Actor actor, final Cell<?> cell,
            final String rawAttributeData) {
        final boolean fill = parser.parseBoolean(rawAttributeData, actor);
        cell.fill(fill, fill);
    }
}
