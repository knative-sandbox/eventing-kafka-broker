/*
 * Copyright 2020 The Knative Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package main

import (
	"context"

	"k8s.io/apimachinery/pkg/runtime/schema"
	"knative.dev/eventing/pkg/logconfig"
	"knative.dev/pkg/configmap"
	"knative.dev/pkg/controller"
	"knative.dev/pkg/injection/sharedmain"
	"knative.dev/pkg/signals"
	"knative.dev/pkg/webhook"
	"knative.dev/pkg/webhook/certificates"
	"knative.dev/pkg/webhook/resourcesemantics"
	"knative.dev/pkg/webhook/resourcesemantics/conversion"
	"knative.dev/pkg/webhook/resourcesemantics/defaulting"
	"knative.dev/pkg/webhook/resourcesemantics/validation"

	"knative.dev/eventing-kafka-broker/control-plane/pkg/apis/eventing"
	eventingv1alpha1 "knative.dev/eventing-kafka-broker/control-plane/pkg/apis/eventing/v1alpha1"
)

var types = map[schema.GroupVersionKind]resourcesemantics.GenericCRD{

	eventingv1alpha1.SchemeGroupVersion.WithKind("KafkaSink"): &eventingv1alpha1.KafkaSink{},
}

var callbacks = map[schema.GroupVersionKind]validation.Callback{}

func NewDefaultingAdmissionController(ctx context.Context, _ configmap.Watcher) *controller.Impl {

	// A function that infuses the context passed to Validate/SetDefaults with custom metadata.
	ctxFunc := func(ctx context.Context) context.Context {
		return ctx
	}

	return defaulting.NewAdmissionController(ctx,
		// Name of the resource webhook.
		"defaulting.webhook.kafka.eventing.knative.dev",

		// The path on which to serve the webhook.
		"/defaulting",

		// The resources to default.
		types,

		// A function that infuses the context passed to Validate/SetDefaults with custom metadata.
		ctxFunc,

		// Whether to disallow unknown fields.
		false,
	)
}

func NewValidationAdmissionController(ctx context.Context, _ configmap.Watcher) *controller.Impl {
	return validation.NewAdmissionController(ctx,
		// Name of the resource webhook.
		"validation.webhook.kafka.eventing.knative.dev",

		// The path on which to serve the webhook.
		"/resource-validation",

		// The resources to validate.
		types,

		// A function that infuses the context passed to Validate/SetDefaults with custom metadata.
		func(ctx context.Context) context.Context {
			return ctx
		},

		// Whether to disallow unknown fields.
		true,

		// Extra validating callbacks to be applied to resources.
		callbacks,
	)
}

func NewConversionController(ctx context.Context, _ configmap.Watcher) *controller.Impl {

	var (
		eventingv1alpha1_ = eventingv1alpha1.SchemeGroupVersion.Version
	)

	return conversion.NewConversionController(ctx,

		// The path on which to serve the webhook
		"/resource-conversion",

		// Specify the types of custom resource definitions that should be converted
		map[schema.GroupKind]conversion.GroupKindConversion{
			eventingv1alpha1.Kind("KafkaSink"): {
				DefinitionName: eventing.KafkaSinksResource.String(),
				HubVersion:     eventingv1alpha1_,
				Zygotes: map[string]conversion.ConvertibleObject{
					eventingv1alpha1_: &eventingv1alpha1.KafkaSink{},
				},
			},
		},

		// A function that infuses the context passed to ConvertTo/ConvertFrom/SetDefaults with custom metadata.
		func(ctx context.Context) context.Context {
			return ctx
		},
	)
}

func main() {

	// Set up a signal context with our webhook options
	ctx := webhook.WithOptions(signals.NewContext(), webhook.Options{
		ServiceName: logconfig.WebhookName(),
		Port:        webhook.PortFromEnv(8443),
		// SecretName must match the name of the Secret created in the configuration.
		SecretName: "kafka-webhook-eventing-certs",
	})

	sharedmain.MainWithContext(ctx, logconfig.WebhookName(),
		certificates.NewController,
		NewDefaultingAdmissionController,
		NewValidationAdmissionController,
		NewConversionController,
	)
}
