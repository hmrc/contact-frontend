package util

trait Feature {
  val name: String
}

sealed trait GetHelpWithThisPageFeature extends Feature

sealed trait GetHelpWithThisPageFeature_A extends GetHelpWithThisPageFeature
sealed trait GetHelpWithThisPageFeature_B extends GetHelpWithThisPageFeature

final case object GetHelpWithThisPageFeature_A extends GetHelpWithThisPageFeature_A {
  override val name: String = "GetHelpWithThisPageFeature_A"
}
final case object GetHelpWithThisPageFeature_B extends GetHelpWithThisPageFeature_B {
  override val name: String = "GetHelpWithThisPageFeature_B"
}