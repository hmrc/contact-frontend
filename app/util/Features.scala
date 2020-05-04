/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package util

sealed trait Feature {
  val name: String
}

abstract class FeatureBase(override val name: String) extends Feature

case object GetHelpWithThisPageNewWordingOfEntryLink extends FeatureBase("GetHelpWithThisPageNewWordingOfEntryLink")
case object GetHelpWithThisPageMoreVerboseHeader extends FeatureBase("GetHelpWithThisPageMoreVerboseHeader")
case object GetHelpWithThisPageMoreVerboseConfirmation extends FeatureBase("GetHelpWithThisPageMoreVerboseConfirmation")
case object GetHelpWithThisPageImprovedFieldValidation extends FeatureBase("GetHelpWithThisPageImprovedFieldValidation")
case object GetHelpWithThisPageNewLargeInputFields extends FeatureBase("GetHelpWithThisPageNewLargeInputFields")
case object GetHelpWithThisPageFeatureFieldHints extends FeatureBase("GetHelpWithThisPageFeatureFieldHints")
case object GetHelpWithThisPageOnlyServerSideValidation
    extends FeatureBase("GetHelpWithThisPageOnlyServerSideValidation")

object Feature {

  val byName: PartialFunction[String, Feature] = {
    case GetHelpWithThisPageMoreVerboseHeader.name        => GetHelpWithThisPageMoreVerboseHeader
    case GetHelpWithThisPageMoreVerboseConfirmation.name  => GetHelpWithThisPageMoreVerboseConfirmation
    case GetHelpWithThisPageImprovedFieldValidation.name  => GetHelpWithThisPageImprovedFieldValidation
    case GetHelpWithThisPageNewLargeInputFields.name      => GetHelpWithThisPageNewLargeInputFields
    case GetHelpWithThisPageFeatureFieldHints.name        => GetHelpWithThisPageFeatureFieldHints
    case GetHelpWithThisPageNewWordingOfEntryLink.name    => GetHelpWithThisPageNewWordingOfEntryLink
    case GetHelpWithThisPageOnlyServerSideValidation.name => GetHelpWithThisPageOnlyServerSideValidation
  }

}
