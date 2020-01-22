package org.smartregister.maternity.presenter;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import org.jeasy.rules.api.Facts;
import org.smartregister.maternity.MaternityLibrary;
import org.smartregister.maternity.R;
import org.smartregister.maternity.contract.MaternityProfileVisitsFragmentContract;
import org.smartregister.maternity.domain.YamlConfig;
import org.smartregister.maternity.domain.YamlConfigItem;
import org.smartregister.maternity.domain.YamlConfigWrapper;
import org.smartregister.maternity.interactor.MaternityProfileVisitsFragmentInteractor;
import org.smartregister.maternity.pojos.OpdVisitSummary;
import org.smartregister.maternity.pojos.OpdVisitSummaryResultModel;
import org.smartregister.maternity.utils.FilePath;
import org.smartregister.maternity.utils.MaternityConstants;
import org.smartregister.maternity.utils.MaternityFactsUtil;
import org.smartregister.maternity.utils.MaternityUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-11-29
 */
public class MaternityProfileVisitsFragmentPresenter implements MaternityProfileVisitsFragmentContract.Presenter {

    private WeakReference<MaternityProfileVisitsFragmentContract.View> mProfileView;
    private MaternityProfileVisitsFragmentContract.Interactor mProfileInteractor;
    private int currentPageNo = 0;
    private int totalPages = 0;

    public MaternityProfileVisitsFragmentPresenter(@NonNull MaternityProfileVisitsFragmentContract.View profileView) {
        mProfileView = new WeakReference<>(profileView);
        mProfileInteractor = new MaternityProfileVisitsFragmentInteractor(this);
    }

    @Override
    public void onDestroy(boolean isChangingConfiguration) {
        mProfileView = null;//set to null on destroy

        // Inform interactor
        if (mProfileInteractor != null) {
            mProfileInteractor.onDestroy(isChangingConfiguration);
        }

        // Activity destroyed set interactor to null
        if (! isChangingConfiguration) {
            mProfileInteractor = null;
        }
    }

    @Override
    public void loadVisits(@NonNull String baseEntityId, @NonNull final OnFinishedCallback onFinishedCallback) {
        if (mProfileInteractor != null) {
            mProfileInteractor.fetchVisits(baseEntityId, currentPageNo, new OnVisitsLoadedCallback() {

                @Override
                public void onVisitsLoaded(@NonNull List<OpdVisitSummary> opdVisitSummaries) {
                    updatePageCounter();

                    ArrayList<Pair<YamlConfigWrapper, Facts>> items = new ArrayList<>();
                    populateWrapperDataAndFacts(opdVisitSummaries, items);
                    onFinishedCallback.onFinished(opdVisitSummaries, items);
                }
            });

        }
    }

    @Override
    public void loadPageCounter(@NonNull String baseEntityId) {
        if (mProfileInteractor != null) {
            mProfileInteractor.fetchVisitsPageCount(baseEntityId, new MaternityProfileVisitsFragmentContract.Interactor.OnFetchVisitsPageCountCallback() {
                @Override
                public void onFetchVisitsPageCount(int visitsPageCount) {
                    totalPages = visitsPageCount;
                    updatePageCounter();
                }
            });
        }
    }

    private void updatePageCounter() {
        String pageCounterTemplate = getString(R.string.current_page_of_total_pages);

        MaternityProfileVisitsFragmentContract.View profileView = getProfileView();
        if (profileView != null && pageCounterTemplate != null) {
            profileView.showPageCountText(String.format(pageCounterTemplate, (currentPageNo + 1), totalPages));

            profileView.showPreviousPageBtn(currentPageNo > 0);
            profileView.showNextPageBtn(currentPageNo < (totalPages -1));
        }
    }

    @Override
    public void populateWrapperDataAndFacts(@NonNull List<OpdVisitSummary> opdVisitSummaries, @NonNull ArrayList<Pair<YamlConfigWrapper, Facts>> items) {
        for (OpdVisitSummary opdVisitSummary: opdVisitSummaries) {
            Facts facts = generateOpdVisitSummaryFact(opdVisitSummary);
            Iterable<Object> ruleObjects = null;

            try {
                ruleObjects = MaternityLibrary.getInstance().readYaml(FilePath.FILE.OPD_VISIT_ROW);
            } catch (IOException e) {
                Timber.e(e);
            }

            if (ruleObjects != null) {
                for (Object ruleObject : ruleObjects) {
                    YamlConfig yamlConfig = (YamlConfig) ruleObject;
                    if (yamlConfig.getGroup() != null) {
                        items.add(new Pair<>(new YamlConfigWrapper(yamlConfig.getGroup(), null, null), facts));
                    }

                    if (yamlConfig.getSubGroup() != null) {
                        items.add(new Pair<>(new YamlConfigWrapper(null, yamlConfig.getSubGroup(), null), facts));
                    }

                    List<YamlConfigItem> configItems = yamlConfig.getFields();

                    if (configItems != null) {
                        for (YamlConfigItem configItem : configItems) {
                            String relevance = configItem.getRelevance();
                            if (relevance != null && MaternityLibrary.getInstance().getMaternityRulesEngineHelper()
                                    .getRelevance(facts, relevance)) {
                                YamlConfigWrapper yamlConfigWrapper = new YamlConfigWrapper(null, null, configItem);
                                items.add(new Pair<>(yamlConfigWrapper, facts));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onNextPageClicked() {
        if (currentPageNo < totalPages && getProfileView() != null && getProfileView().getClientBaseEntityId() != null) {
            currentPageNo++;

            loadVisits(getProfileView().getClientBaseEntityId(), new OnFinishedCallback() {
                @Override
                public void onFinished(@NonNull List<OpdVisitSummary> opdVisitSummaries, @NonNull ArrayList<Pair<YamlConfigWrapper, Facts>> items) {
                    if (getProfileView() != null) {
                        getProfileView().displayVisits(opdVisitSummaries, items);
                    }
                }
            });
        }
    }

    @Override
    public void onPreviousPageClicked() {
        if (currentPageNo > 0 && getProfileView() != null && getProfileView().getClientBaseEntityId() != null) {
            currentPageNo--;

            loadVisits(getProfileView().getClientBaseEntityId(), new OnFinishedCallback() {
                @Override
                public void onFinished(@NonNull List<OpdVisitSummary> opdVisitSummaries, @NonNull ArrayList<Pair<YamlConfigWrapper, Facts>> items) {
                    if (getProfileView() != null) {
                        getProfileView().displayVisits(opdVisitSummaries, items);
                    }
                }
            });
        }
    }

    @NonNull
    private Facts generateOpdVisitSummaryFact(@NonNull OpdVisitSummary opdVisitSummary) {
        Facts facts = new Facts();

        if (opdVisitSummary.getVisitDate() != null) {
            MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.VISIT_DATE, MaternityUtils.convertDate(opdVisitSummary.getVisitDate(), MaternityConstants.DateFormat.d_MMM_yyyy));
        }

        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.TEST_NAME, opdVisitSummary.getTestName());
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.TEST_RESULT, opdVisitSummary.getTestResult());
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.DIAGNOSIS, opdVisitSummary.getDiagnosis());
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.DIAGNOSIS_TYPE, opdVisitSummary.getDiagnosisType());

        // Put the diseases text
        String diseasesText = generateDiseasesText(opdVisitSummary);
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.DISEASE_CODE, diseasesText);

        // Put the treatment text
        HashMap<String, OpdVisitSummaryResultModel.Treatment> treatments = opdVisitSummary.getTreatments();
        String medicationText = generateMedicationText(treatments);
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.TREATMENT, medicationText);

        // Add translate-able labels
        setLabelsInFacts(facts);

        return facts;
    }

    @Override
    @NonNull
    public String generateMedicationText(@NonNull HashMap<String, OpdVisitSummaryResultModel.Treatment> treatments) {
        StringBuilder stringBuilder = new StringBuilder();
        for (OpdVisitSummaryResultModel.Treatment treatment : treatments.values()) {
            if (treatment != null && treatment.getMedicine() != null) {
                String medicine = treatment.getMedicine();
                if (stringBuilder.length() > 1) {
                    stringBuilder.append("<br/>");
                }

                String medicationTemplate = getString(R.string.single_medicine_visit_preview_summary);
                String doseOrDurationHtml = getString(R.string.dose_or_duration_html);

                if (medicationTemplate != null && !TextUtils.isEmpty(medicine)) {
                    stringBuilder.append(String.format(medicationTemplate
                            , medicine));

                    StringBuilder doseAndDurationText = new StringBuilder();
                    String dosage = treatment.getDosage();
                    if (!TextUtils.isEmpty(dosage)) {
                        String medicationDoseTemplate = getString(R.string.medication_dose);
                        if (medicationDoseTemplate != null) {
                            doseAndDurationText.append(String.format(medicationDoseTemplate, dosage));

                            if (!TextUtils.isEmpty(treatment.getDuration())) {
                                doseAndDurationText.append(". ");
                            }
                        }
                    }

                    String duration = treatment.getDuration();
                    if (!TextUtils.isEmpty(duration)) {
                        String medicationDurationTemplate = getString(R.string.medication_duration);
                        if (medicationDurationTemplate != null) {
                            doseAndDurationText.append(String.format(medicationDurationTemplate, duration));
                        }
                    }

                    if (doseAndDurationText.length() > 0 && doseOrDurationHtml != null) {
                        stringBuilder.append(String.format(doseOrDurationHtml, doseAndDurationText.toString()));
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    @Override
    @NonNull
    public String generateDiseasesText(@NonNull OpdVisitSummary opdVisitSummary) {
        HashSet<String> diseases = opdVisitSummary.getDiseases();
        Iterator<String> diseaseIterator = diseases.iterator();

        StringBuilder stringBuilder = new StringBuilder();

        while (diseaseIterator.hasNext()) {
            String disease = diseaseIterator.next();
            if (!TextUtils.isEmpty(disease)) {
                stringBuilder.append(disease);

                if (diseaseIterator.hasNext()) {
                    stringBuilder.append("\n");
                }
            }
        }
        return stringBuilder.toString();
    }

    private void setLabelsInFacts(@NonNull Facts facts) {
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.DIAGNOSIS_LABEL, getString(R.string.diagnosis));
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.DIAGNOSIS_TYPE_LABEL, getString(R.string.diagnosis_type));
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.DISEASE_CODE_LABEL, getString(R.string.disease_code));
        MaternityFactsUtil.putNonNullFact(facts, MaternityConstants.FactKey.OpdVisit.TREATMENT_LABEL, getString(R.string.treatment));
    }

    @Nullable
    @Override
    public MaternityProfileVisitsFragmentContract.View getProfileView() {
        if (mProfileView != null) {
            return mProfileView.get();
        } else {
            return null;
        }
    }

    @Nullable
    public String getString(@StringRes int stringId) {
        MaternityProfileVisitsFragmentContract.View profileView = getProfileView();
        if (profileView != null) {
            return profileView.getString(stringId);
        }

        return null;
    }
}