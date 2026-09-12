package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.model.ActionItem;
import com.mediscan.mediscan.agent.model.ConflictItem;
import com.mediscan.mediscan.agent.model.FollowUpRecord;
import com.mediscan.mediscan.agent.model.ValidationResult;
import com.mediscan.mediscan.agent.model.ValidationResult.RuleCheck;

import java.util.List;

/**
 * Tool interface for automated rule-based clinical record validation.
 */
public interface RecordValidationService {

    ValidationResult validateRecord(FollowUpRecord record,
                                    List<ConflictItem> conflicts,
                                    List<ActionItem> actions);

    // Independently-testable individual rule checks
    RuleCheck checkRequiredFields(FollowUpRecord record, List<ActionItem> actions);

    RuleCheck checkHighSeverityConflicts(List<ConflictItem> conflicts);

    RuleCheck checkTemporalValueConsistency(FollowUpRecord record);

    RuleCheck checkGuardrailSafety(FollowUpRecord record);

    RuleCheck checkPhysiologicalBounds(FollowUpRecord record);
}
