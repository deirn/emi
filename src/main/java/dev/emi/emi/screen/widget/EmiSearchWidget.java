package dev.emi.emi.screen.widget;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiConfig;
import dev.emi.emi.search.EmiSearch;
import dev.emi.emi.search.QueryType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

public class EmiSearchWidget extends TextFieldWidget {
	private static final Pattern ESCAPE = Pattern.compile("\\\\.");
	private static List<Pair<Integer, Style>> styles;

	public EmiSearchWidget(TextRenderer textRenderer, int x, int y, int width, int height) {
		super(textRenderer, x, y, width, height, new LiteralText(""));
		this.setFocusUnlocked(true);
		this.setEditableColor(-1);
		this.setUneditableColor(-1);
		this.setDrawsBackground(true);
		this.setMaxLength(256);
		this.setRenderTextProvider((string, stringStart) -> {
			MutableText text = null;
			int s = 0;
			int last = 0;
			for (; s < styles.size(); s++) {
				Pair<Integer, Style> style = styles.get(s);
				int end = style.getLeft();
				if (end > stringStart) {
					if (end - stringStart >= string.length()) {
						text = new LiteralText(string.substring(0, string.length())).setStyle(style.getRight());
						// Skip second loop
						s = styles.size();
						break;
					}
					text = new LiteralText(string.substring(0, end - stringStart)).setStyle(style.getRight());
					last = end - stringStart;
					s++;
					break;
				}
			}
			for (; s < styles.size(); s++) {
				Pair<Integer, Style> style = styles.get(s);
				int end = style.getLeft();
				if (end - stringStart >= string.length()) {
					text.append(new LiteralText(string.substring(last, string.length())).setStyle(style.getRight()));
					break;
				}
				text.append(new LiteralText(string.substring(last, end - stringStart)).setStyle(style.getRight()));
				last = end - stringStart;
			}
			return text.asOrderedText();
		});
		this.setChangedListener(string -> {
			if (string.isEmpty()) {
				this.setSuggestion("Search EMI...");
			} else {
				this.setSuggestion("");
			}
			Matcher matcher = EmiSearch.TOKENS.matcher(string);
			List<Pair<Integer, Style>> styles = Lists.newArrayList();
			int last = 0;
			while (matcher.find()) {
				int start = matcher.start();
				int end = matcher.end();
				if (last < start) {
					styles.add(new Pair<Integer, Style>(start, Style.EMPTY.withFormatting(Formatting.WHITE)));
				}
				String group = matcher.group();
				QueryType type = QueryType.fromString(group);
				int subStart = type.prefix.length();
				if (group.length() > 1 + subStart && group.substring(subStart).startsWith("/") && group.endsWith("/")) {
					int rOff = start + subStart + 1;
					styles.add(new Pair<Integer, Style>(rOff, type.slashColor));
					Matcher rMatcher = ESCAPE.matcher(string.substring(rOff, end - 1));
					int rLast = 0;
					while (rMatcher.find()) {
						int rStart = rMatcher.start();
						int rEnd = rMatcher.end();
						if (rLast < rStart) {
							styles.add(new Pair<Integer, Style>(rStart + rOff, type.regexColor));
						}
						styles.add(new Pair<Integer, Style>(rEnd + rOff, type.escapeColor));
						rLast = rEnd;
					}
					if (rLast < end - 1) {
						styles.add(new Pair<Integer, Style>(end - 1, type.regexColor));
					}
					styles.add(new Pair<Integer, Style>(end, type.slashColor));
				} else {
					styles.add(new Pair<Integer, Style>(end, type.color));
				}

				last = end;
			}
			if (last < string.length()) {
				styles.add(new Pair<Integer, Style>(string.length(), Style.EMPTY.withFormatting(Formatting.WHITE)));
			}
			EmiSearchWidget.styles = styles;
			EmiSearch.search(string);
		});
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean b = super.mouseClicked(mouseX, mouseY, button);
		if (this.isFocused() && button == 1) {
			this.setText("");
		}
		return b;
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.setEditable(EmiConfig.enabled);
		if (EmiConfig.enabled) {
			super.render(matrices, mouseX, mouseY, delta);
		}
	}
}