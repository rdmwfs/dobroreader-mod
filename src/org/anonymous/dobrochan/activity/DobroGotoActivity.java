package org.anonymous.dobrochan.activity;

import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.clear.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;

public class DobroGotoActivity extends GDActivity {
	EditText board_edit;
	EditText display_id_edit;
	
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if(position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, "home");
			startActivity(i);
			return true;
		}
		return super.onHandleActionBarItemClick(item, position);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DobroHelper.updateCurrentTheme(this);
		DobroHelper.setOrientation(this);
		super.onCreate(savedInstanceState);
		setActionBarContentView(R.layout.goto_simple_view);
		board_edit = (EditText) findViewById(R.id.gt_board);
		display_id_edit = (EditText) findViewById(R.id.gt_display_id);
		Button btn = (Button) findViewById(R.id.goto_button);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (board_edit.getText().length() == 0
						|| display_id_edit.getText().length() == 0) {
					Toast.makeText(DobroGotoActivity.this,
							getText(R.string.values_not_filled), Toast.LENGTH_SHORT).show();
				} else {
					Intent i = new Intent(DobroGotoActivity.this,
							DobroPostActivity.class);
					i.putExtra(DobroConstants.BOARD, board_edit.getText().toString());
					i.putExtra(DobroConstants.POST, display_id_edit.getText().toString());
					i.putExtra(GD_ACTION_BAR_TITLE, String.format(">>%s/%s",
							board_edit.getText(), display_id_edit.getText()).toString());
					startActivity(i);
				}

			}
		});
	}

}
