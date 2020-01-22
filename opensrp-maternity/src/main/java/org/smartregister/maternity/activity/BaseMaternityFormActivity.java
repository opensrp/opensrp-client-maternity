package org.smartregister.maternity.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.activities.JsonWizardFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.maternity.MaternityLibrary;
import org.smartregister.maternity.R;
import org.smartregister.maternity.dao.OpdDiagnosisAndTreatmentFormDao;
import org.smartregister.maternity.fragment.BaseMaternityFormFragment;
import org.smartregister.maternity.pojos.OpdDiagnosisAndTreatmentForm;
import org.smartregister.maternity.utils.AppExecutors;
import org.smartregister.maternity.utils.MaternityConstants;
import org.smartregister.maternity.utils.MaternityJsonFormUtils;
import org.smartregister.maternity.utils.MaternityUtils;
import org.smartregister.util.LangUtils;
import org.smartregister.util.Utils;

import java.util.HashMap;
import java.util.Set;

import timber.log.Timber;

public class BaseMaternityFormActivity extends JsonWizardFormActivity {

    private BaseMaternityFormFragment opdFormFragment;
    private boolean enableOnCloseDialog = true;
    private JSONObject form;

    private HashMap<String, String> parcelableData = new HashMap<>();

    @Override
    protected void attachBaseContext(android.content.Context base) {

        String language = LangUtils.getLanguage(base);
        super.attachBaseContext(LangUtils.setAppLocale(base, language));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            form = new JSONObject(currentJsonState());
        } catch (JSONException e) {
            Timber.e(e);
        }

        enableOnCloseDialog = getIntent().getBooleanExtra(MaternityConstants.FormActivity.EnableOnCloseDialog, true);

        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            Set<String> keySet = extras.keySet();

            for (String key : keySet) {
                if (!key.equals(MaternityConstants.JSON_FORM_EXTRA.JSON)) {
                    Object objectValue = extras.get(key);

                    if (objectValue instanceof String) {
                        String value = (String) objectValue;
                        parcelableData.put(key, value);
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            String encounterType = form.getString(MaternityJsonFormUtils.ENCOUNTER_TYPE);
            confirmCloseTitle = getString(R.string.confirm_form_close);
            confirmCloseMessage = encounterType.trim().toLowerCase().contains("update") ? this.getString(R.string.any_changes_you_make) : this.getString(R.string.confirm_form_close_explanation);
            setConfirmCloseTitle(confirmCloseTitle);
            setConfirmCloseMessage(confirmCloseMessage);

        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public void initializeFormFragment() {
        initializeFormFragmentCore();
    }

    protected void initializeFormFragmentCore() {
        opdFormFragment = (BaseMaternityFormFragment) BaseMaternityFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction().add(com.vijay.jsonwizard.R.id.container, opdFormFragment).commit();
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        if (toolbar != null) {
            toolbar.setContentInsetStartWithNavigation(0);
        }
        super.setSupportActionBar(toolbar);
    }

    @Override
    public void writeValue(String stepName, String key, String value, String openMrsEntityParent, String openMrsEntity,
                           String openMrsEntityId, boolean popup) throws JSONException {
        super.writeValue(stepName, key, value, openMrsEntityParent, openMrsEntity, openMrsEntityId, popup);
    }

    @Override
    public void writeValue(String stepName, String parentKey, String childObjectKey, String childKey, String value,
                           String openMrsEntityParent, String openMrsEntity, String openMrsEntityId, boolean popup)
            throws JSONException {
        super.writeValue(stepName, parentKey, childObjectKey, childKey, value, openMrsEntityParent, openMrsEntity,
                openMrsEntityId, popup);
    }

    @Override
    public void writeValue(String stepName, String key, String value, String openMrsEntityParent, String openMrsEntity,
                           String openMrsEntityId) throws JSONException {
        super.writeValue(stepName, key, value, openMrsEntityParent, openMrsEntity, openMrsEntityId);
    }

    @Override
    public void writeValue(String stepName, String parentKey, String childObjectKey, String childKey, String value,
                           String openMrsEntityParent, String openMrsEntity, String openMrsEntityId) throws JSONException {
        super.writeValue(stepName, parentKey, childObjectKey, childKey, value, openMrsEntityParent, openMrsEntity,
                openMrsEntityId);
    }

    /**
     * Conditionaly display the confirmation dialog
     */
    @Override
    public void onBackPressed() {
        if (enableOnCloseDialog) {
            if (form.optString(MaternityJsonFormUtils.ENCOUNTER_TYPE).equals(MaternityConstants.EventType.DIAGNOSIS_AND_TREAT)) {
                AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppThemeAlertDialog).setTitle(confirmCloseTitle)
                        .setMessage(getString(R.string.save_form_fill_session))
                        .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveFormFillSession();
                                BaseMaternityFormActivity.this.finish();
                            }
                        }).setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Timber.d("No button on dialog in %s", JsonFormActivity.class.getCanonicalName());
                            }
                        }).setNeutralButton(getString(R.string.end_session), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BaseMaternityFormActivity.this.finish();
                            }
                        }).create();

                dialog.show();
            } else {
                super.onBackPressed();
            }

        } else {
            BaseMaternityFormActivity.this.finish();
        }
    }

    private void saveFormFillSession() {
        JSONObject jsonObject = getmJSONObject();
        final OpdDiagnosisAndTreatmentForm opdDiagnosisAndTreatmentForm = new OpdDiagnosisAndTreatmentForm(0, MaternityUtils.getIntentValue(getIntent(), MaternityConstants.IntentKey.BASE_ENTITY_ID),
                jsonObject.toString(), Utils.convertDateFormat(new DateTime()));
        final OpdDiagnosisAndTreatmentFormDao opdDiagnosisAndTreatmentFormDao = MaternityLibrary.getInstance().getOpdDiagnosisAndTreatmentFormRepository();
        new AppExecutors().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                opdDiagnosisAndTreatmentFormDao.saveOrUpdate(opdDiagnosisAndTreatmentForm);
            }
        });
    }

    @NonNull
    public HashMap<String, String> getParcelableData() {
        return parcelableData;
    }
}